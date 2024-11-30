package corgitaco.betterweather.weather.event.client.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.CloudyClient;
import net.minecraft.util.GsonHelper;

public class CloudyClientSettings extends WeatherClientSettings {

    public static final Codec<CloudyClientSettings> CODEC = RecordCodecBuilder.create((builder) -> commonFields(builder)
            .apply(builder, CloudyClientSettings::new));

    public CloudyClientSettings(JsonObject json) {
        this(
                new ColorSettings(GsonHelper.getAsJsonObject(json, "colorSettings")),
                GsonHelper.getAsFloat(json, "skyOpacity"),
                GsonHelper.getAsFloat(json, "fogDensity"),
                GsonHelper.getAsBoolean(json, "sunsetSunriseColor"),
                LegacyWeatherRendering.valueOf(GsonHelper.getAsString(json, "renderingType"))
        );
    }

    public CloudyClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor) {
        this(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, LegacyWeatherRendering.CLEAR);
    }

    public CloudyClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, LegacyWeatherRendering weatherRendering) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, weatherRendering);
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new CloudyClient(this);
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.CLOUDY_CLIENT;
    }
}
