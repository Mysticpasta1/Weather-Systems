package corgitaco.betterweather.weather.event.client.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.NoneClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class SunnyClientSettings extends WeatherClientSettings {
    public static final Codec<SunnyClientSettings> CODEC = RecordCodecBuilder.create((builder) -> builder.group(ColorSettings.CODEC.fieldOf("colorSettings").forGetter(WeatherClientSettings::getColorSettings)).apply(builder, SunnyClientSettings::new));

    public SunnyClientSettings(JsonObject json) {
        this(
                new ColorSettings(GsonHelper.getAsJsonObject(json, "colorSettings"))
        );
    }


    public SunnyClientSettings(ColorSettings colorSettings) {
        super(colorSettings, 1.0F, -1.0F, true, LegacyWeatherRendering.CLEAR);
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new NoneClient(this);
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.SUNNY_CLIENT;
    }
}
