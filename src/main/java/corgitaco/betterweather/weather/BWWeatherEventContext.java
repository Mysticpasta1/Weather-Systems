package corgitaco.betterweather.weather;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.Climate;
import corgitaco.betterweather.api.season.Season;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.api.weather.WeatherEventContext;
import corgitaco.betterweather.api.weather.WeatherEventSettings;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.data.network.NetworkHandler;
import corgitaco.betterweather.data.network.packet.util.RefreshRenderersPacket;
import corgitaco.betterweather.data.network.packet.weather.WeatherDataPacket;
import corgitaco.betterweather.data.storage.WeatherEventSavedData;
import corgitaco.betterweather.helpers.BiomeUpdate;
import corgitaco.betterweather.util.TomlCommentedConfigOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class BWWeatherEventContext implements WeatherEventContext {

    public static final String CONFIG_NAME = "weather-settings.toml";
    private static final String DEFAULT = "betterweather-none";

    public static final Codec<BWWeatherEventContext> PACKET_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.STRING.fieldOf("currentEvent").forGetter((weatherEventContext) -> {
            return weatherEventContext.currentEvent.getName();
        }), Codec.BOOL.fieldOf("weatherForced").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherForced;
        }), ResourceLocation.CODEC.fieldOf("worldID").forGetter((weatherEventContext) -> {
            return weatherEventContext.worldID;
        }), Codec.unboundedMap(Codec.STRING, WeatherEvent.CODEC).fieldOf("weatherEvents").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherEvents;
        })).apply(builder, BWWeatherEventContext::new);
    });

    public static final TomlCommentedConfigOps CONFIG_OPS = new TomlCommentedConfigOps(Util.make(new HashMap<>(), (map) -> {
        map.put("changeBiomeColors", "Do weather events change biome vegetation colors? This will cause chunks to refresh (F3+A).");
    }), true);


    private final Map<String, WeatherEvent> weatherEvents = new HashMap<>();
    private final ResourceLocation worldID;
    private final Registry<Biome> biomeRegistry;
    private final Path weatherConfigPath;
    private final Path weatherEventsConfigPath;
    private final File weatherConfigFile;

    private boolean refreshRenderers;
    private WeatherEvent currentEvent;
    private boolean weatherForced;

    //Packet Constructor
    public BWWeatherEventContext(String currentEvent, boolean weatherForced, ResourceLocation worldID, Map<String, WeatherEvent> weatherEvents) {
        this(currentEvent, weatherForced, worldID, null, weatherEvents);
    }

    //Server world constructor
    public BWWeatherEventContext(WeatherEventSavedData weatherEventSavedData, RegistryKey<World> worldID, Registry<Biome> biomeRegistry) {
        this(weatherEventSavedData.getEvent(), weatherEventSavedData.isWeatherForced(), worldID.getLocation(), biomeRegistry, null);
    }

    public BWWeatherEventContext(String currentEvent, boolean weatherForced, ResourceLocation worldID, @Nullable Registry<Biome> biomeRegistry, @Nullable Map<String, WeatherEvent> weatherEvents) {
        this.worldID = worldID;
        this.biomeRegistry = biomeRegistry;
        this.weatherConfigPath = BetterWeather.CONFIG_PATH.resolve(worldID.getNamespace()).resolve(worldID.getPath()).resolve("weather");
        this.weatherEventsConfigPath = this.weatherConfigPath.resolve("events");
        this.weatherConfigFile = this.weatherConfigPath.resolve(CONFIG_NAME).toFile();
        this.weatherEvents.put(DEFAULT, WeatherEvent.NONE.setName(DEFAULT));
        this.weatherForced = weatherForced;
        boolean isClient = weatherEvents != null;
        boolean isPacket = biomeRegistry == null;

        if (isClient) {
            this.weatherEvents.putAll(weatherEvents);
        }
        if (!isPacket) {
            this.handleConfig(isClient);
        }


        WeatherEvent currentWeatherEvent = this.weatherEvents.get(currentEvent);
        this.currentEvent = this.weatherEvents.getOrDefault(currentEvent, WeatherEvent.NONE);

        if (currentEvent != null && currentWeatherEvent == null) {
            BetterWeather.LOGGER.error("The last weather event for the world: \"" + worldID.toString() + "\" was not found in: \"" + this.weatherEventsConfigPath.toString() + "\".\nDefaulting to weather event: \"" + DEFAULT + "\".");
        } else {
            if (!isClient && !isPacket) {
                BetterWeather.LOGGER.info(worldID.toString() + " initialized with a weather event of: \"" + (currentEvent == null ? DEFAULT : currentEvent) + "\".");
            }
        }
        if (!isPacket) {
            for (Map.Entry<String, WeatherEvent> stringWeatherEventEntry : this.weatherEvents.entrySet()) {
                stringWeatherEventEntry.getValue().fillBiomes(biomeRegistry);
            }
        }
    }


    public void tick(World world) {
        WeatherEvent prevEvent = this.currentEvent;
        boolean wasForced = this.weatherForced;
        if (world instanceof ServerWorld) {
            shuffleAndPickWeatherEvent(world);
        }

        if (prevEvent != this.currentEvent || wasForced != this.weatherForced) {
            onWeatherChange(world);
        }
        if (world instanceof ServerWorld) {
            this.currentEvent.worldTick((ServerWorld) world, world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED), world.getGameTime());
        }
        if (world.isRemote) {
            this.currentEvent.clientTick((ClientWorld) world, world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED), world.getGameTime(), Minecraft.getInstance());
        }
    }

    private void onWeatherChange(World world) {
        ((BiomeUpdate) world).updateBiomeData();
        save(world);
        if (world instanceof ServerWorld) {
            ((IServerWorldInfo) world.getWorldInfo()).setThundering(this.currentEvent.isThundering());
            sendPackets((ServerWorld) world);
        }
    }

    private void sendPackets(ServerWorld world) {
        NetworkHandler.sendToAllPlayers(world.getPlayers(), new WeatherDataPacket(this));
        if (this.refreshRenderers) {
            NetworkHandler.sendToAllPlayers(world.getPlayers(), new RefreshRenderersPacket());
        }
    }

    private void shuffleAndPickWeatherEvent(World world) {
        boolean isPrecipitation = world.getWorldInfo().isRaining();
        Season season = ((Climate) world).getSeason();
        boolean hasSeasons = season != null;
        float rainingStrength = world.rainingStrength;
        if (isPrecipitation) {
            if (rainingStrength <= 0.02F) {
                if (!this.weatherForced) {
                    Random random = new Random(((ServerWorld) world).getSeed() + world.getGameTime());
                    ArrayList<String> list = new ArrayList<>(this.weatherEvents.keySet());
                    Collections.shuffle(list, random);
                    for (String entry : list) {
                        if (entry.equals(DEFAULT)) {
                            continue;
                        }
                        WeatherEvent weatherEvent = this.weatherEvents.get(entry);
                        double chance = hasSeasons ? weatherEvent.getSeasonChances().getOrDefault(season.getKey(), new IdentityHashMap<>()).getOrDefault(season.getPhase(), weatherEvent.getDefaultChance()) : weatherEvent.getDefaultChance();

                        if (random.nextDouble() < chance || this.currentEvent == this.weatherEvents.get(DEFAULT)) {
                            this.currentEvent = weatherEvent;
                            break;
                        }
                    }
                }
            }
        } else {
            if (rainingStrength == 0.0F) {
                this.currentEvent = this.weatherEvents.get(DEFAULT);
                this.weatherForced = false;
            }
        }
    }

    private void save(World world) {
        WeatherEventSavedData weatherEventSavedData = WeatherEventSavedData.get(world);
        weatherEventSavedData.setEvent(this.currentEvent.getName());
        weatherEventSavedData.setWeatherForced(this.weatherForced);
    }

    public WeatherEvent weatherForcer(String weatherEventName, int weatherEventLength, ServerWorld world) {
        this.currentEvent = this.weatherEvents.get(weatherEventName);
        this.weatherForced = true;

        IServerWorldInfo worldInfo = (IServerWorldInfo) world.getWorldInfo();
        boolean isDefault = weatherEventName.equals(DEFAULT);

        if (isDefault) {
            worldInfo.setClearWeatherTime(weatherEventLength);
        } else {
            worldInfo.setClearWeatherTime(0);
            worldInfo.setRainTime(weatherEventLength);
            worldInfo.setRaining(true);
            worldInfo.setThundering(this.currentEvent.isThundering());
        }

        onWeatherChange(world);
        return this.currentEvent;
    }


    public void handleConfig(boolean isClient) {
        if (!this.weatherConfigFile.exists()) {
            createDefaultWeatherConfigFile();
        } else {
            try (Reader reader = new FileReader(this.weatherConfigFile)) {
                Optional<WeatherEventConfig> configHolder = WeatherEventConfig.CODEC.parse(CONFIG_OPS, new TomlParser().parse(reader)).resultOrPartial(BetterWeather.LOGGER::error);

                if (configHolder.isPresent()) {
                    this.refreshRenderers = configHolder.get().changeBiomeColors;
                } else {
                    BetterWeather.LOGGER.error("\"" + this.weatherConfigFile.toString() + "\" not there when requested.");
                }
            } catch (IOException e) {
                BetterWeather.LOGGER.error(e.toString());
            }
        }

        handleEventConfigs(isClient);
    }

    private void createDefaultWeatherConfigFile() {
        CommentedConfig readConfig = this.weatherConfigFile.exists() ? CommentedFileConfig.builder(this.weatherConfigFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
        if (readConfig instanceof CommentedFileConfig) {
            ((CommentedFileConfig) readConfig).load();
        }

        CommentedConfig encodedConfig = (CommentedConfig) WeatherEventConfig.CODEC.encodeStart(CONFIG_OPS, WeatherEventConfig.DEFAULT).result().get();
        try {
            Files.createDirectories(this.weatherConfigFile.toPath().getParent());
            new TomlWriter().write(this.weatherConfigFile.exists() ? TomlCommentedConfigOps.recursivelyUpdateAndSortConfig(readConfig, encodedConfig) : encodedConfig, this.weatherConfigFile, WritingMode.REPLACE);
        } catch (IOException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }

    private void handleEventConfigs(boolean isClient) {
        File eventsDirectory = this.weatherEventsConfigPath.toFile();
        if (!eventsDirectory.exists()) {
            createDefaultEventConfigs();
        }

        File[] files = eventsDirectory.listFiles();

        if (files.length == 0) {
            createDefaultEventConfigs();
        }

        iterateAndReadConfiguredEvents(eventsDirectory.listFiles(), isClient);
    }

    private void iterateAndReadConfiguredEvents(File[] files, boolean isClient) {
        for (File configFile : files) {
            String absolutePath = configFile.getAbsolutePath();
            if (absolutePath.endsWith(".toml")) {
                CommentedConfig readConfig = configFile.exists() ? CommentedFileConfig.builder(configFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
                if (readConfig instanceof CommentedFileConfig) {
                    ((CommentedFileConfig) readConfig).load();
                }
                String name = configFile.getName().replace(".toml", "").toLowerCase();
                WeatherEvent decodedValue = WeatherEvent.CODEC.decode(TomlCommentedConfigOps.INSTANCE, readConfig).resultOrPartial(BetterWeather.LOGGER::error).get().getFirst().setName(name);

                if (isClient) {
                    if (this.weatherEvents.containsKey(name)) {
                        this.weatherEvents.get(name).setClientSettings(decodedValue.getClientSettings());
                    }
                } else {
                    this.weatherEvents.put(name, decodedValue);
                }

            } else if (absolutePath.endsWith(".json")) {
                try {
                    String name = configFile.getName().replace(".json", "").toLowerCase();
                    WeatherEvent decodedValue = WeatherEvent.CODEC.decode(JsonOps.INSTANCE, new JsonParser().parse(new FileReader(configFile))).resultOrPartial(BetterWeather.LOGGER::error).get().getFirst().setName(name);
                    if (isClient) {
                        if (this.weatherEvents.containsKey(name)) {
                            this.weatherEvents.get(name).setClientSettings(decodedValue.getClientSettings());
                        }
                    } else {
                        this.weatherEvents.put(name, decodedValue);
                    }
                } catch (FileNotFoundException e) {
                    BetterWeather.LOGGER.error(e.toString());
                }
            }
        }
    }

    private void createTomlEventConfig(WeatherEvent weatherEvent, ResourceLocation weatherEventID) {
        Path configFile = this.weatherEventsConfigPath.resolve(weatherEventID.toString().replace(":", "-") + ".json");
        CommentedConfig readConfig = configFile.toFile().exists() ? CommentedFileConfig.builder(configFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
        if (readConfig instanceof CommentedFileConfig) {
            ((CommentedFileConfig) readConfig).load();
        }
        CommentedConfig encodedConfig = (CommentedConfig) WeatherEvent.CODEC.encodeStart(weatherEvent.configOps(), weatherEvent).result().get();

        try {
            Files.createDirectories(configFile.getParent());
            new TomlWriter().write(configFile.toFile().exists() ? TomlCommentedConfigOps.recursivelyUpdateAndSortConfig(readConfig, encodedConfig) : encodedConfig, configFile, WritingMode.REPLACE);
        } catch (IOException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }

    private void createJsonEventConfig(WeatherEvent weatherEvent, ResourceLocation weatherEventID) {
        Path configFile = this.weatherEventsConfigPath.resolve(weatherEventID.toString().replace(":", "-") + ".json");
        CommentedConfig readConfig = configFile.toFile().exists() ? CommentedFileConfig.builder(configFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
        if (readConfig instanceof CommentedFileConfig) {
            ((CommentedFileConfig) readConfig).load();
        }
        JsonElement jsonElement = WeatherEvent.CODEC.encodeStart(JsonOps.INSTANCE, weatherEvent).result().get();

        try {
            Files.createDirectories(configFile.getParent());
            Files.write(configFile, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonElement).getBytes());
        } catch (IOException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }


    public void createDefaultEventConfigs() {
        for (Map.Entry<ResourceLocation, WeatherEvent> entry : WeatherEvent.DEFAULT_EVENTS.entrySet()) {
            ResourceLocation location = entry.getKey();
            WeatherEvent event = entry.getValue();
            Optional<RegistryKey<Codec<? extends WeatherEvent>>> optionalKey = BetterWeatherRegistry.WEATHER_EVENT.getOptionalKey(event.codec());

            if (optionalKey.isPresent()) {
                if (BetterWeatherConfig.SERIALIZE_AS_JSON) {
                    createJsonEventConfig(event, location);
                } else {
                    createTomlEventConfig(event, location);
                }
            } else {
                throw new IllegalStateException("Weather Event Key for codec not there when requested: " + event.getClass().getSimpleName());
            }
        }
    }

    public void setCurrentEvent(WeatherEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public void setCurrentEvent(String currentEvent) {
        this.currentEvent = this.weatherEvents.get(currentEvent);
    }

    public void setWeatherForced(boolean weatherForced) {
        this.weatherForced = weatherForced;
    }

    public WeatherEvent getCurrentEvent() {
        return currentEvent;
    }

    public Map<String, WeatherEvent> getWeatherEvents() {
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
    public String getCurrentWeatherEventKey() {
        return this.currentEvent.getName();
    }

    @Override
    public WeatherEventSettings getCurrentWeatherEventSettings() {
        return this.currentEvent;
    }

    public boolean isRefreshRenderers() {
        return refreshRenderers;
    }

    private static class WeatherEventConfig {
        public static final WeatherEventConfig DEFAULT = new WeatherEventConfig(true);

        public static Codec<WeatherEventConfig> CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.BOOL.fieldOf("changeBiomeColors").forGetter((weatherEventConfig) -> {
                return weatherEventConfig.changeBiomeColors;
            })).apply(builder, WeatherEventConfig::new);
        });

        private final boolean changeBiomeColors;

        private WeatherEventConfig(boolean changeBiomeColors) {
            this.changeBiomeColors = changeBiomeColors;
        }
    }
}
