package corgitaco.betterweather.api.weather;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.client.settings.*;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

import java.util.Objects;
import java.util.function.Function;

public abstract class WeatherClientSettings {
    public static final Codec<WeatherClientSettings> CODEC = WeatherClientSettingType.CODEC.dispatch(WeatherClientSettings::type, WeatherClientSettingType::codec);
    public static <T extends WeatherClientSettings> Products.P5<RecordCodecBuilder.Mu<T>, ColorSettings, Float, Float, Boolean, LegacyWeatherRendering> commonFields(RecordCodecBuilder.Instance<T> builder) {
        return builder.group(ColorSettings.CODEC.optionalFieldOf("colorSettings", new ColorSettings("#00ff00", 0.5, "#00ff00", 0.5)).forGetter(t -> Objects.requireNonNull(t.getColorSettings(), "Color")),
                Codec.FLOAT.optionalFieldOf("skyOpacity", 1.0F).forGetter(t -> Objects.requireNonNull(t.skyOpacity(), "Sky")),
                Codec.FLOAT.optionalFieldOf("fogDensity", -1.0F).forGetter(t -> Objects.requireNonNull(t.fogDensity(), "Fog")),
                Codec.BOOL.optionalFieldOf("sunsetSunriseColor", true).forGetter(t -> Objects.requireNonNull(t.sunsetSunriseColor(), "Sunset")),
                StringRepresentable.fromEnum(LegacyWeatherRendering::values).optionalFieldOf("renderingType", LegacyWeatherRendering.CLEAR).forGetter(t -> Objects.requireNonNull(t.renderingType(), "Rendering")));
    }


    private final ColorSettings colorSettings;
    private final float skyOpacity;
    private final float fogDensity;
    private final boolean sunsetSunriseColor;
    private final LegacyWeatherRendering renderingType;

    public WeatherClientSettings(JsonObject json) {
        this(
                new ColorSettings(GsonHelper.getAsJsonObject(json, "colorSettings")),
                GsonHelper.getAsFloat(json, "skyOpacity"),
                GsonHelper.getAsFloat(json, "fogDensity"),
                GsonHelper.getAsBoolean(json, "sunsetSunriseColor"),
                LegacyWeatherRendering.valueOf(GsonHelper.getAsString(json, "renderingType"))
        );
    }

    public WeatherClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, LegacyWeatherRendering renderingType) {
        this.colorSettings = colorSettings;
        this.skyOpacity = skyOpacity;
        this.fogDensity = fogDensity;
        this.sunsetSunriseColor = sunsetSunriseColor;
        this.renderingType = renderingType;
    }

    public static WeatherClientSettings fromJson(JsonObject json) {
        var type = GsonHelper.getAsString(json, "type");

        return WeatherClientSettingType.REGISTRY.get(new ResourceLocation(type)).function().apply(json);
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

    public LegacyWeatherRendering renderingType() {
        return renderingType;
    }

    public enum LegacyWeatherRendering implements StringRepresentable {
        CLEAR, RAIN, ACIDIC, SNOWY;

        private final String name;

        LegacyWeatherRendering() {
            this.name = name().toLowerCase();
        }


        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record WeatherClientSettingType<T extends WeatherClientSettings>(Function<JsonObject, T> function, Codec<T> codec) {
        public static final BiMap<ResourceLocation, WeatherClientSettingType<?>> REGISTRY = HashBiMap.create();
        public static final Codec<WeatherClientSettingType<?>> CODEC =  ResourceLocation.CODEC.xmap(REGISTRY::get, weatherType -> REGISTRY.inverse().get(weatherType));

        public static final WeatherClientSettingType<AcidRainClientSettings> ACID_RAIN_CLIENT = register("acid_rain", AcidRainClientSettings::new, AcidRainClientSettings.CODEC);
        public static final WeatherClientSettingType<SnowClientSettings> SNOW_CLIENT = register("snow", SnowClientSettings::new, SnowClientSettings.CODEC);
        public static final WeatherClientSettingType<CloudyClientSettings> CLOUDY_CLIENT = register("cloudy", CloudyClientSettings::new, CloudyClientSettings.CODEC);
        public static final WeatherClientSettingType<SunnyClientSettings> SUNNY_CLIENT = register("sunny", SunnyClientSettings::new, SunnyClientSettings.CODEC);
        public static final WeatherClientSettingType<RainClientSettings> RAIN_CLIENT = register("rain", RainClientSettings::new, RainClientSettings.CODEC);

        public static <T extends WeatherClientSettings> WeatherClientSettingType<T> register(String name, Function<JsonObject, T> decode, Codec<T> codec) {
            var type = new WeatherClientSettingType<>(decode, codec);
            REGISTRY.put(new ResourceLocation(BetterWeather.MOD_ID, name), type);
            return type;
        }

        public static void register() {}
    }
}
