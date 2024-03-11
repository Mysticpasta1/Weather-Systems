package corgitaco.betterweather.api.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.util.BetterWeatherUtil;
import corgitaco.betterweather.weather.event.None;
import corgitaco.betterweather.weather.event.client.NoneClient;
import corgitaco.betterweather.weather.event.client.settings.NoneClientSettings;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class WeatherEvent implements WeatherEventSettings {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final boolean MODIFY_TEMPERATURE = false;

    public static final Codec<WeatherEvent> CODEC =  ExtraCodecs.lazyInitializedCodec(() -> BetterWeatherRegistry.WEATHER_EVENT.get().getCodec()).dispatchStable(WeatherEvent::codec, Function.identity());

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherEventClientSettings.VALUE_COMMENTS), (map) -> {
        map.put("defaultChance", "What is the default chance for this weather event to occur? This value is only used when Seasons are NOT present in the given dimension.");
        map.put("temperatureOffset", "What is the temperature offset for valid biomes?");
        map.put("humidityOffset", "What is the temperature offset for valid biomes?");
        map.put("isThundering", "Determines whether or not this weather event may spawn lightning and sets world info internally for MC and mods to use.");
        map.put("lightningChance", "How often does lightning spawn? Requires \"isThundering\" to be true.");
        map.put("type", "Target Weather Event's Registry ID to configure settings for in this config.");
        map.put("biomeCondition", "Better Weather uses a prefix system for what biomes weather is allowed to function in.\n Prefix Guide:\n \"#\" - Biome category representable.\n \"$\" - Biome dictionary representable.\n \",\" - Creates a new condition, separate from the previous.\n \"ALL\" - Spawn in all biomes(no condition).\n \"!\" - Negates/flips/does the reverse of the condition.\n \"\" - No prefix serves as a biome ID OR Mod ID representable.\n\n Here are a few examples:\n1. \"byg#THE_END, $OCEAN\" would mean that the ore may spawn in biomes with the name space \"byg\" AND in the \"END\" biome category, OR all biomes in the \"OCEAN\" dictionary.\n2. \"byg:guiana_shield, #MESA\" would mean that the ore may spawn in the \"byg:guiana_shield\" OR all biomes in the \"MESA\" category.\n3. \"byg#ICY$MOUNTAIN\" would mean that the ore may only spawn in biomes from byg in the \"ICY\" category and \"MOUNTAIN\" dictionary type.\n4. \"!byg#DESERT\" would mean that the ore may only spawn in biomes that are NOT from byg and NOT in the \"DESERT\" category.\n5. \"ALL\", spawn everywhere. \n6. \"\" Don't spawn anywhere.");
    });

    private final String biomeCondition;
    private final double defaultChance;
    private final double temperatureOffsetRaw;
    private final double humidityOffsetRaw;
    private final boolean isThundering;
    private final int lightningFrequency;
    private final ReferenceArraySet<Biome> validBiomes = new ReferenceArraySet<>();
    private WeatherEventClientSettings clientSettings;
    private WeatherEventClient<?> client;
    private String name;

    private final NoneClientSettings noneClientSettings = new NoneClientSettings(new ColorSettings("0B8649", 3.0, "0B8649", 3.0));

    public WeatherEvent(WeatherEventClientSettings clientSettings, String biomeCondition, double defaultChance, double temperatureOffsetRaw, double humidityOffsetRaw, boolean isThundering, int lightningFrequency) {
        this.clientSettings = clientSettings;
        this.biomeCondition = biomeCondition;
        this.defaultChance = defaultChance;
        this.temperatureOffsetRaw = temperatureOffsetRaw;
        this.humidityOffsetRaw = humidityOffsetRaw;
        this.isThundering = isThundering;
        this.lightningFrequency = lightningFrequency;
    }

    public final double getDefaultChance() {
        return defaultChance;
    }

    public abstract void worldTick(ServerLevel world, int tickSpeed, long worldTime);

    public abstract Codec<? extends WeatherEvent> codec();

    public abstract DynamicOps<?> configOps();

    public void livingEntityUpdate(Entity entity) {
    }

    /**
     * This is called in the chunk ticking iterator.
     */
    public void chunkTick(LevelChunk chunk, ServerLevel world) {
    }

    public final void doChunkTick(LevelChunk chunk, ServerLevel world) {
        chunkTick(chunk, world);
    }

    public void onChunkLoad(ChunkHolder chunk, ServerLevel world) {

    }

    public final void doChunkLoad(ChunkHolder chunk, ServerLevel world) {
        onChunkLoad(chunk, world);
    }

    public boolean fillBlocksWithWater() {
        return false;
    }

    public boolean spawnSnowInFreezingClimates() {
        return true;
    }

    public final Component successTranslationTextComponent(String key) {
        return Component.translatable("commands.bw.setweather.success", Component.translatable("bw.weather." + key));
    }

    public String getName() {
        return name;
    }

    public WeatherEvent setName(String name) {
        this.name = name;
        return this;
    }

    public void fillBiomes(Registry<Biome> biomeRegistry) {
        Set<Map.Entry<ResourceKey<Biome>, Biome>> entries = biomeRegistry.entrySet();

        for (Map.Entry<ResourceKey<Biome>, Biome> entry : entries) {
            Biome biome = entry.getValue();
            ResourceKey<Biome> key = entry.getKey();

            this.validBiomes.add(biome);
        }
    }

    public WeatherEventClientSettings getClientSettings() {
        if(clientSettings != null) {
            return clientSettings;
        } else {
            return noneClientSettings;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public WeatherEvent setClientSettings(WeatherEventClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        return this;
    }

    public String getBiomeCondition() {
        return biomeCondition;
    }

    public boolean isValidBiome(Biome biome) {
        return this.validBiomes.contains(biome);
    }

    public double getTemperatureOffsetRaw() {
        return temperatureOffsetRaw;
    }

    public double getHumidityOffsetRaw() {
        return humidityOffsetRaw;
    }

    public boolean isThundering() {
        return isThundering;
    }

    public int getLightningChance() {
        return lightningFrequency;
    }

    @OnlyIn(Dist.CLIENT)
    public WeatherEventClient<?> getClient() {
        if(client != null) {
            return client;
        } else {
            return noneClientSettings.createClientSettings();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setClient(WeatherEventClient<?> client) {
        this.client = client;
    }
}