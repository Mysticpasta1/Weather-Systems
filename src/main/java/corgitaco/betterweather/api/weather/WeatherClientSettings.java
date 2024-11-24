package corgitaco.betterweather.api.weather;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherClientSettingType;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.client.settings.CloudyClientSettings;
import net.minecraft.Util;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public abstract class WeatherClientSettings {
    public static <T extends WeatherClientSettings> Products.P4<RecordCodecBuilder.Mu<T>, ColorSettings, Float, Float, Boolean> commonFields(RecordCodecBuilder.Instance<T> builder) {
        return builder.group(ColorSettings.CODEC.fieldOf("colorSettings").forGetter(WeatherClientSettings::getColorSettings),
                Codec.FLOAT.fieldOf("skyOpacity").forGetter(WeatherClientSettings::skyOpacity),
                Codec.FLOAT.fieldOf("fogDensity").forGetter(WeatherClientSettings::fogDensity),
                Codec.BOOL.fieldOf("sunsetSunriseColor").forGetter(WeatherClientSettings::sunsetSunriseColor));
    }

    public static final Codec<WeatherClientSettings> CODEC = WeatherClientSettingType.CODEC;

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
}
