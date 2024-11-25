package corgitaco.betterweather.weather.event.client.settings;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.RainClient;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class RainClientSettings extends WeatherClientSettings {


    public static final Codec<RainClientSettings> CODEC = RecordCodecBuilder.create(builder -> rainFields(builder)
            .apply(builder, RainClientSettings::new));

    public static <T extends RainClientSettings> Products.P6<RecordCodecBuilder.Mu<T>, ColorSettings, Float, Float, Boolean, ResourceLocation, ResourceLocation> rainFields(RecordCodecBuilder.Instance<T> builder) {
        return commonFields(builder)
                .and(ResourceLocation.CODEC.fieldOf("rainTexture").forGetter(blizzardClientSettings -> blizzardClientSettings.rainTexture))
                .and(ResourceLocation.CODEC.fieldOf("snowTexture").forGetter(blizzardClientSettings -> blizzardClientSettings.snowTexture));
    }

    public final ResourceLocation rainTexture;
    public final ResourceLocation snowTexture;

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherClientSettings.VALUE_COMMENTS), (map) -> {
        map.put("rainTexture", "Texture path to the rain texture.");
        map.put("snowTexture", "Texture path to the rain texture.");
    });

    public RainClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation rainTexture, ResourceLocation snowTexture) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor);
        this.rainTexture = rainTexture;
        this.snowTexture = snowTexture;
    }
    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new RainClient(this);
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.RAIN_CLIENT.get();
    }
}
