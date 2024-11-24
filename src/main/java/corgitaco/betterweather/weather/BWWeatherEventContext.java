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
import corgitaco.betterweather.WeatherType;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherEventContext;
import corgitaco.betterweather.api.weather.WeatherEventSettings;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.data.network.NetworkHandler;
import corgitaco.betterweather.data.network.packet.util.RefreshRenderersPacket;
import corgitaco.betterweather.data.network.packet.weather.WeatherDataPacket;
import corgitaco.betterweather.data.storage.WeatherEventSavedData;
import corgitaco.betterweather.helpers.ClientBiomeUpdate;
import corgitaco.betterweather.helpers.ServerBiomeUpdate;
import corgitaco.betterweather.util.TomlCommentedConfigOps;
import corgitaco.betterweather.weather.event.DefaultEvents;
import corgitaco.betterweather.weather.event.Sunny;
import net.minecraft.Util;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BWWeatherEventContext implements WeatherEventContext {

    public static final String CONFIG_NAME = "weather-settings.toml";
    private static final String DEFAULT = "none";

    public static final Codec<BWWeatherEventContext> PACKET_CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(Codec.STRING.fieldOf("currentEvent").forGetter((weatherEventContext) -> {
            return weatherEventContext.currentEvent.getName();
        }), Codec.BOOL.fieldOf("weatherForced").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherForced;
        }), ResourceLocation.CODEC.fieldOf("worldID").forGetter((weatherEventContext) -> {
            return weatherEventContext.worldID;
        }), Codec.unboundedMap(Codec.STRING, Weather.CODEC).fieldOf("weatherEvents").forGetter((weatherEventContext) -> {
            return weatherEventContext.weatherEvents;
        })).apply(builder, BWWeatherEventContext::new);
    });

    public static final TomlCommentedConfigOps CONFIG_OPS = new TomlCommentedConfigOps(Util.make(new HashMap<>(), (map) -> {
        map.put("changeBiomeColors", "Do weather events change biome vegetation colors? This will cause chunks to refresh (F3+A).");
    }), true);


    private static final Map<String, Weather> weatherEvents = new HashMap<>();
    private final ResourceLocation worldID;
    private final Path weatherConfigPath;
    private final Path weatherEventsConfigPath;
    private final File weatherConfigFile;

    private boolean refreshRenderers;
    public static Weather currentEvent;
    private boolean weatherForced;

    //Packet Constructor
    public BWWeatherEventContext(String currentEvent, boolean weatherForced, ResourceLocation worldID, Map<String, Weather> weatherEvents) {
        this(currentEvent, weatherForced, worldID, null, weatherEvents);
    }

    //Server world constructor
    public BWWeatherEventContext(WeatherEventSavedData weatherEventSavedData, ResourceKey<Level> worldID, Registry<Biome> biomeRegistry) {
        this(weatherEventSavedData.getEvent(), weatherEventSavedData.isWeatherForced(), worldID.location(), biomeRegistry, null);
    }

    public BWWeatherEventContext(String currentEvent, boolean weatherForced, ResourceLocation worldID, @Nullable Registry<Biome> biomeRegistry, @Nullable Map<String, Weather> weatherEvents) {
        this.worldID = worldID;
        this.weatherConfigPath = BetterWeather.CONFIG_PATH.resolve(worldID.getNamespace()).resolve(worldID.getPath()).resolve("weather");
        this.weatherEventsConfigPath = this.weatherConfigPath.resolve("events");
        this.weatherConfigFile = this.weatherConfigPath.resolve(CONFIG_NAME).toFile();
        BWWeatherEventContext.weatherEvents.put(DEFAULT, DefaultEvents.SUNNY.setName(DEFAULT));
        this.weatherForced = weatherForced;
        boolean isClient = weatherEvents != null;
        boolean isPacket = biomeRegistry == null;

        if (isClient) {
            BWWeatherEventContext.weatherEvents.putAll(weatherEvents);

            BWWeatherEventContext.weatherEvents.forEach((key, weatherEvent) -> {
                weatherEvent.setClient(weatherEvent.getClientSettings().createClientSettings());
            });
        }
        if (!isPacket) {
            this.handleConfig(isClient);
        }

        Weather currentWeather = BWWeatherEventContext.weatherEvents.get(currentEvent);
        BWWeatherEventContext.currentEvent = BWWeatherEventContext.weatherEvents.getOrDefault(currentEvent, DefaultEvents.SUNNY);
        if (currentEvent != null && currentWeather == null) {
            BetterWeather.LOGGER.error("The last weather event for the world: \"" + worldID.toString() + "\" was not found in: \"" + this.weatherEventsConfigPath.toString() + "\".\nDefaulting to weather event: \"" + DEFAULT + "\".");
        } else {
            if (!isClient && !isPacket) {
                BetterWeather.LOGGER.info(worldID.toString() + " initialized with a weather event of: \"" + (currentEvent == null ? DEFAULT : currentEvent) + "\".");
            }
        }
        if (!isPacket) {
            for (Map.Entry<String, Weather> stringWeatherEventEntry : BWWeatherEventContext.weatherEvents.entrySet()) {
                stringWeatherEventEntry.getValue().fillBiomes(biomeRegistry);
            }
        }
    }


    public void tick(Level world) {
        //TODO: Remove this check and figure out what could possibly be causing this and prevent it.
        if (weatherEvents.get(DEFAULT) == this.getCurrentEvent() && world.isRaining()) {
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
                ((ServerLevelData) world.getLevelData()).setThundering(currentEvent.isThundering());
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
                    ArrayList<String> list = new ArrayList<>(weatherEvents.keySet());
                    Collections.shuffle(list, random);
                    for (String entry : list) {
                        if (entry.equals(DEFAULT)) {
                            continue;
                        }
                        Weather weather = weatherEvents.get(entry);
                        double chance =  weather.getDefaultChance();

                        if (random.nextDouble() < chance || currentEvent == weatherEvents.get(DEFAULT)) {
                            currentEvent = weather;
                            break;
                        }
                    }
                }
            }
        } else {
            if (rainingStrength == 0.0F) {
                currentEvent = weatherEvents.get(DEFAULT);
                this.weatherForced = false;
            }
        }
    }

    private void save(Level world) {
        WeatherEventSavedData weatherEventSavedData = WeatherEventSavedData.get(world);
        weatherEventSavedData.setEvent(this.getCurrentEvent().getName());
        weatherEventSavedData.setWeatherForced(this.weatherForced);
    }

    public Weather weatherForcer(String weatherEventName, int weatherEventLength, ServerLevel world) {
        currentEvent = weatherEvents.get(weatherEventName);
        this.weatherForced = true;

        ServerLevelData worldInfo = (ServerLevelData) world.getLevelData();
        boolean isDefault = weatherEventName.equals(DEFAULT);

        if (isDefault) {
            worldInfo.setClearWeatherTime(weatherEventLength);
        } else if (weatherEventName.equals("none")) {
            worldInfo.setClearWeatherTime(0);
            worldInfo.setRaining(false);
        } else {
            worldInfo.setClearWeatherTime(0);
            worldInfo.setRainTime(weatherEventLength);
            worldInfo.setRaining(true);
            worldInfo.setThundering(currentEvent.isThundering());
        }

        onWeatherChange(world);
        return currentEvent;
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

        if (isClient) {
            addSettingsIfMissing();
        }

        iterateAndReadConfiguredEvents(eventsDirectory.listFiles(), isClient);
    }

    private void iterateAndReadConfiguredEvents(File[] files, boolean isClient) {
        for (File configFile : files) {
            String absolutePath = configFile.getAbsolutePath();
            if (absolutePath.endsWith(".toml")) {
                readToml(isClient, configFile);

            } else if (absolutePath.endsWith(".json")) {
                readJson(isClient, configFile);
            }
        }
    }

    private void readJson(boolean isClient, File configFile) {
        try {
            String name = configFile.getName().replace(".json", "").toLowerCase();
            Weather decodedValue = Weather.CODEC.decode(JsonOps.INSTANCE, new JsonParser().parse(new FileReader(configFile))).resultOrPartial(BetterWeather.LOGGER::error).get().getFirst().setName(name);
            if (isClient && !BetterWeather.CLIENT_CONFIG.useServerClientSettings) {
                if (weatherEvents.containsKey(name)) {
                    Weather weather = weatherEvents.get(name);
                    weather.setClientSettings(decodedValue.getClientSettings());
                    weather.setClient(weather.getClientSettings().createClientSettings());
                }
            } else {
                weatherEvents.put(name, decodedValue);
            }
        } catch (FileNotFoundException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }

    private void readToml(boolean isClient, File configFile) {
        CommentedConfig readConfig = configFile.exists() ? CommentedFileConfig.builder(configFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
        if (readConfig instanceof CommentedFileConfig) {
            ((CommentedFileConfig) readConfig).load();
        }
        String name = configFile.getName().replace(".toml", "").toLowerCase();
        Weather decodedValue = Weather.CODEC.decode(TomlCommentedConfigOps.INSTANCE, readConfig).resultOrPartial(BetterWeather.LOGGER::error).get().getFirst().setName(name);

        if (isClient && !BetterWeather.CLIENT_CONFIG.useServerClientSettings) {
            if (weatherEvents.containsKey(name)) {
                Weather weather = weatherEvents.get(name);
                weather.setClientSettings(decodedValue.getClientSettings());
                weather.setClient(weather.getClientSettings().createClientSettings());
            }
        } else {
            weatherEvents.put(name, decodedValue);
        }
    }

    private void createTomlEventConfig(Weather weather, String weatherEventID) {
        Path configFile = this.weatherEventsConfigPath.resolve(weatherEventID.replace(":", "-") + ".toml");
        CommentedConfig readConfig = configFile.toFile().exists() ? CommentedFileConfig.builder(configFile).sync().autosave().writingMode(WritingMode.REPLACE).build() : CommentedConfig.inMemory();
        if (readConfig instanceof CommentedFileConfig) {
            ((CommentedFileConfig) readConfig).load();
        }
        CommentedConfig encodedConfig = (CommentedConfig) Weather.CODEC.encodeStart(CONFIG_OPS, weather).result().get();

        try {
            Files.createDirectories(configFile.getParent());
            new TomlWriter().write(configFile.toFile().exists() ? TomlCommentedConfigOps.recursivelyUpdateAndSortConfig(readConfig, encodedConfig) : encodedConfig, configFile, WritingMode.REPLACE);
        } catch (IOException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }

    private void createJsonEventConfig(Weather weather, String weatherEventID) {
        Path configFile = this.weatherEventsConfigPath.resolve(weatherEventID.replace(":", "-") + ".json");
        JsonElement jsonElement = Weather.CODEC.encodeStart(JsonOps.INSTANCE, weather).result().get();

        try {
            Files.createDirectories(configFile.getParent());
            Files.write(configFile, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonElement).getBytes());
        } catch (IOException e) {
            BetterWeather.LOGGER.error(e.toString());
        }
    }


    public void createDefaultEventConfigs() {
        for (Map.Entry<ResourceLocation, Weather> entry : BetterWeatherRegistry.DEFAULT_EVENTS.entrySet()) {
            ResourceLocation location = entry.getKey();
            Weather event = entry.getValue();
            Optional<ResourceKey<WeatherType<?>>> optionalKey = WeatherType.REGISTRY.get().getResourceKey(event.type());

            if (optionalKey.isPresent()) {
                if (BetterWeatherConfig.SERIALIZE_AS_JSON) {
                    createJsonEventConfig(event, location.toString());
                } else {
                    createTomlEventConfig(event, location.toString());
                }
            } else {
                throw new IllegalStateException("Weather Event Key for codec not there when requested: " + event.getClass().getSimpleName());
            }
        }
    }

    public static Map<String, Weather> getWeatherEvent() {
        return weatherEvents;
    }

    @OnlyIn(Dist.CLIENT)
    public void addSettingsIfMissing() {
        for (Map.Entry<String, Weather> entry : weatherEvents.entrySet()) {
            Weather event = entry.getValue();
            String key = entry.getKey();
            File tomlFile = this.weatherEventsConfigPath.resolve(key + ".toml").toFile();
            File jsonFile = this.weatherEventsConfigPath.resolve(key + ".json").toFile();
            Optional<ResourceKey<WeatherType<?>>> optionalKey = WeatherType.REGISTRY.get().getResourceKey(event.type());

            if (optionalKey.isPresent()) {
                if (!tomlFile.exists() && !jsonFile.exists()) {
                    if (BetterWeatherConfig.SERIALIZE_AS_JSON) {
                        createJsonEventConfig(event, key);
                    } else {
                        createTomlEventConfig(event, key);
                    }
                }
            } else {
                throw new IllegalStateException("Weather Event Key for codec not there when requested: " + event.getClass().getSimpleName());
            }
        }
    }

    public void setCurrentEvent(Weather currentEvent) {
        BWWeatherEventContext.currentEvent = currentEvent;
    }

    public void setCurrentEvent(String currentEvent) {
        BWWeatherEventContext.currentEvent = weatherEvents.get(currentEvent);
    }

    public void setWeatherForced(boolean weatherForced) {
        this.weatherForced = weatherForced;
    }

    public Weather getCurrentEvent() {
        if(currentEvent != null) {
            return currentEvent;
        } else {
            if(!weatherEvents.isEmpty()) {
                return weatherEvents.values().iterator().next();
            } else {
                BWWeatherEventContext.weatherEvents.put(DEFAULT, DefaultEvents.SUNNY.setName(DEFAULT));
                return BWWeatherEventContext.weatherEvents.get(DEFAULT);
            }
        }
    }

    public Map<String, Weather> getWeatherEvents() {
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
        if(this.getCurrentEvent().getName() != null) {
            return this.getCurrentEvent().getName();
        } else {
            return "none";
        }
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
        return this.getCurrentEvent().getClient();
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
