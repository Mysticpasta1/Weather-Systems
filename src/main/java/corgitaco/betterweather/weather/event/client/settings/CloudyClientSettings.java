package corgitaco.betterweather.weather.event.client.settings;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherClientSettingType;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.CloudyClient;

public class CloudyClientSettings extends WeatherClientSettings {

    public static final Codec<CloudyClientSettings> CODEC = RecordCodecBuilder.create((builder) -> commonFields(builder)
            .apply(builder, CloudyClientSettings::new));


    public CloudyClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor);
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new CloudyClient(this);
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.CLOUDY_CLIENT.get();
    }
}
