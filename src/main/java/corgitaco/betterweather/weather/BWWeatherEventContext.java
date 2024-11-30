package corgitaco.betterweather.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherEventContext;
import corgitaco.betterweather.api.weather.WeatherEventSettings;
import corgitaco.betterweather.data.network.NetworkHandler;
import corgitaco.betterweather.data.network.packet.util.RefreshRenderersPacket;
import corgitaco.betterweather.data.network.packet.weather.WeatherDataPacket;
import corgitaco.betterweather.data.storage.WeatherEventSavedData;
import corgitaco.betterweather.helpers.ClientBiomeUpdate;
import corgitaco.betterweather.helpers.ServerBiomeUpdate;
import corgitaco.betterweather.weather.event.DefaultEvents;
import corgitaco.betterweather.weather.event.Sunny;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

public class BWWeatherEventContext implements WeatherEventContext {

    public static final Codec<BWWeatherEventContext> PACKET_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(ResourceLocation.CODEC.fieldOf("currentEvent").forGetter((weatherEventContext) -> {
            return weatherEventContext.currentEvent;
        }), Codec.BOOL.fieldOf("weatherForced").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherForced;
        }), Codec.unboundedMap(ResourceLocation.CODEC, Weather.CODEC).fieldOf("weatherEvents").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherEvents;
        })).apply(builder, BWWeatherEventContext::new);
    });


    private final Map<ResourceLocation, Weather> weatherEvents = new HashMap<>();

    private boolean refreshRenderers;
    public ResourceLocation currentEvent;
    private boolean weatherForced;

    //Packet Constructor
    public BWWeatherEventContext(ResourceLocation currentEvent, boolean weatherForced, Map<ResourceLocation, Weather> weatherEvents) {
        this(currentEvent, weatherForced, null, null, weatherEvents);
    }

    //Server world constructor
    public BWWeatherEventContext(WeatherEventSavedData weatherEventSavedData) {
        this(weatherEventSavedData.getEvent(), weatherEventSavedData.isWeatherForced(), null, null, null);
    }

    public BWWeatherEventContext(ResourceLocation currentEvent, boolean weatherForced, ResourceKey<Level> worldId, @Nullable Registry<Biome> biomeRegistry, @Nullable Map<ResourceLocation, Weather> weatherEvents) {
        this.currentEvent = currentEvent;
        this.weatherForced = weatherForced;
        boolean isClient = weatherEvents != null;
        boolean isPacket = biomeRegistry == null;

        if (isClient) {
            this.weatherEvents.putAll(weatherEvents);

            weatherEvents.forEach((key, weatherEvent) -> {
                weatherEvent.initClient();
            });
        }


        if (!isPacket) {
            for (Map.Entry<ResourceLocation, Weather> stringWeatherEventEntry : weatherEvents.entrySet()) {
                stringWeatherEventEntry.getValue().fillBiomes(biomeRegistry);
            }
        }
    }


    public void tick(Level world) {
        //TODO: Remove this check and figure out what could possibly be causing this and prevent it.
        if (this.getCurrentEvent() == null && world.isRaining()) {
            world.getLevelData().setRaining(false);
        }

        Weather prevEvent = this.getCurrentEvent();
        boolean wasForced = this.weatherForced;
        if (world instanceof ServerLevel level) {
            shuffleAndPickWeatherEvent(level);
        }

        if (prevEvent != this.getCurrentEvent() || wasForced != this.weatherForced) {
            onWeatherChange(world);
        }
        if (world instanceof ServerLevel level) {
            this.getCurrentEvent().worldTick(level, world.getGameRules().getInt(GameRules.RULE_RANDOMTICKING), world.getGameTime());
        }
        if (world.isClientSide) {
            this.getCurrentClientEvent().clientTick((ClientLevel) world, world.getGameRules().getInt(GameRules.RULE_RANDOMTICKING), world.getGameTime(), Minecraft.getInstance(), getCurrentEvent()::isValidBiome);
        }
    }

    private void onWeatherChange(Level world) {
        if (world.getChunkSource() instanceof ServerChunkCache serverChunkCache) {
            new ServerBiomeUpdate(serverChunkCache, world.registryAccess(), this).updateBiomeData();
            save(world);
            if (world instanceof ServerLevel serverLevel) {
                ((ServerLevelData) world.getLevelData()).setThundering(getCurrentEvent().isThundering());
                sendPackets(serverLevel);
            }
        } else {
            new ClientBiomeUpdate(world.registryAccess(), this).updateBiomeData();
            save(world);
        }
    }

    private void sendPackets(ServerLevel world) {
        NetworkHandler.sendToAllPlayers(world.players(), new WeatherDataPacket(this));
        if (this.refreshRenderers) {
            NetworkHandler.sendToAllPlayers(world.players(), new RefreshRenderersPacket());
        }
    }

    private void shuffleAndPickWeatherEvent(Level world) {
        boolean isPrecipitation = world.getLevelData().isRaining();
        float rainingStrength = world.rainLevel;
        if (isPrecipitation) {
            if (rainingStrength <= 0.02F) {
                if (!this.weatherForced) {
                    Random random = new Random(((ServerLevel) world).getSeed() + world.getGameTime());
                    List<ResourceLocation> list = new ArrayList<>(weatherEvents.keySet());
                    Collections.shuffle(list, random);
                    for (ResourceLocation entry : list) {
                        if (entry.equals(currentEvent)) {
                            continue;
                        }
                        Weather weather = weatherEvents.get(entry);
                        double chance =  weather.getDefaultChance();

                        if (random.nextDouble() < chance || currentEvent == Sunny.KEY) {
                            currentEvent = entry;
                            break;
                        }
                    }
                }
            }
        } else {
            if (rainingStrength == 0.0F) {
                currentEvent = null;
                this.weatherForced = false;
            }
        }
    }

    private void save(Level world) {
        WeatherEventSavedData weatherEventSavedData = WeatherEventSavedData.get(world);
        weatherEventSavedData.setEvent(this.getCurrentWeatherEventKey());
        weatherEventSavedData.setWeatherForced(this.weatherForced);
    }

    public Weather weatherForcer(ResourceLocation weatherEventName, int weatherEventLength, ServerLevel world) {
        currentEvent = weatherEventName;
        var weather = getCurrentEvent();

        this.weatherForced = true;

        ServerLevelData worldInfo = (ServerLevelData) world.getLevelData();

        if (weatherEventName.equals(Sunny.KEY)) {
            worldInfo.setClearWeatherTime(weatherEventLength);
            worldInfo.setRaining(false);
        } else {
            worldInfo.setClearWeatherTime(0);
            worldInfo.setRainTime(weatherEventLength);
            worldInfo.setRaining(true);
            worldInfo.setThundering(weather.isThundering());
        }

        onWeatherChange(world);
        return weather;
    }


    public void setCurrentEvent(ResourceLocation currentEvent) {
        this.currentEvent = currentEvent;
    }

    public void setWeatherForced(boolean weatherForced) {
        this.weatherForced = weatherForced;
    }

    public Weather getCurrentEvent() {
        if(currentEvent != null) {
            return weatherEvents.get(currentEvent);
        } else {
            return Sunny.INSTANCE;
        }
    }

    public Map<ResourceLocation, Weather> getWeatherEvents() {
        return weatherEvents;
    }

    public boolean isWeatherForced() {
        return weatherForced;
    }

    @Override
    public boolean isLocalizedWeather() {
        return false;
    }

    @Override
    public ResourceLocation getCurrentWeatherEventKey() {
        return Objects.requireNonNullElse(currentEvent, Sunny.KEY);
    }

    @Override
    public Weather getCurrentWeatherEntry() {
        return getCurrentEvent();
    }

    @Override
    public WeatherEventSettings getCurrentWeatherEventSettings() {
        return this.getCurrentEvent();
    }

    public boolean isRefreshRenderers() {
        return refreshRenderers;
    }

    @OnlyIn(Dist.CLIENT)
    public WeatherEventClient<?> getCurrentClientEvent() {
        var weather = this.getCurrentEvent();
        return weather != null ? weather.getClient() : null;
    }

}
