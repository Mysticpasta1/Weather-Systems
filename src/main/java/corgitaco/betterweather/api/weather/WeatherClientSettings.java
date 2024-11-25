package corgitaco.betterweather.api.weather;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.client.settings.*;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public abstract class WeatherClientSettings {
    public static final Codec<WeatherClientSettings> CODEC = ExtraCodecs.lazyInitializedCodec(() -> WeatherClientSettingType.CODEC.dispatch(WeatherClientSettings::type, WeatherClientSettingType::codec));

    public static <T extends WeatherClientSettings> Products.P4<RecordCodecBuilder.Mu<T>, ColorSettings, Float, Float, Boolean> commonFields(RecordCodecBuilder.Instance<T> builder) {
        return builder.group(ColorSettings.CODEC.fieldOf("colorSettings").forGetter(WeatherClientSettings::getColorSettings),
                Codec.FLOAT.fieldOf("skyOpacity").forGetter(WeatherClientSettings::skyOpacity),
                Codec.FLOAT.fieldOf("fogDensity").forGetter(WeatherClientSettings::fogDensity),
                Codec.BOOL.fieldOf("sunsetSunriseColor").forGetter(WeatherClientSettings::sunsetSunriseColor));
    }

    private final ColorSettings colorSettings;
    private final float skyOpacity;
    private final float fogDensity;
    private final boolean sunsetSunriseColor;

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(ColorSettings.VALUE_COMMENTS), (map) -> {
        map.put("skyOpacity", "What is the opacity of the sky? 0.0 means hidden, 1.0 is fully visible.\n#Range 0.0-1.0");
        map.put("fogDensity", "How dense is fog?");
        map.put("sunsetSunriseColor", "Do sunsets/sunrises modify fog/sky color?");
    });

    public WeatherClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor) {
        this.colorSettings = colorSettings;
        this.skyOpacity = skyOpacity;
        this.fogDensity = fogDensity;
        this.sunsetSunriseColor = sunsetSunriseColor;
    }

    public abstract WeatherEventClient<?> createClientSettings();

    public boolean sunsetSunriseColor() {
        return sunsetSunriseColor;
    }

    public float skyOpacity() {
        return Mth.clamp(skyOpacity, 0.0F, 1.0F);
    }

    public float dayLightDarkness() {
        return fogDensity;
    }

    public boolean drippingLeaves() {
        return false;
    }

    public float fogDensity() {
        return fogDensity;
    }

    public ColorSettings getColorSettings() {
        return colorSettings;
    }

    abstract public WeatherClientSettingType<?> type();

    public record WeatherClientSettingType<T extends WeatherClientSettings>(Codec<T> codec) {
        public static final Registrar<WeatherClientSettingType<?>> REGISTRY = RegistrarManager.get(BetterWeather.MOD_ID).<WeatherClientSettingType<?>>builder(new ResourceLocation(BetterWeather.MOD_ID, "abstract_pocket_type")).build();
        public static final Codec<WeatherClientSettingType<?>> CODEC =  ResourceLocation.CODEC.xmap(REGISTRY::get, REGISTRY::getId);

        public static final RegistrySupplier<WeatherClientSettingType<AcidRainClientSettings>> ACID_RAIN_CLIENT = register("acid_rain", AcidRainClientSettings.CODEC);
        public static final RegistrySupplier<WeatherClientSettingType<SnowClientSettings>> SNOW_CLIENT = register("snow", SnowClientSettings.CODEC);
        public static final RegistrySupplier<WeatherClientSettingType<CloudyClientSettings>> CLOUDY_CLIENT = register("cloudy", CloudyClientSettings.CODEC);
        public static final RegistrySupplier<WeatherClientSettingType<SunnyClientSettings>> SUNNY_CLIENT = register("sunny", SunnyClientSettings.CODEC);
        public static final RegistrySupplier<WeatherClientSettingType<RainClientSettings>> RAIN_CLIENT = register("rain", RainClientSettings.CODEC);

        public static <T extends WeatherClientSettings> RegistrySupplier<WeatherClientSettingType<T>> register(String name, Codec<T> codec) {
            return REGISTRY.register(new ResourceLocation(BetterWeather.MOD_ID, name), () -> new WeatherClientSettingType<>(codec));
        }

        public static void register() {}
    }
}
