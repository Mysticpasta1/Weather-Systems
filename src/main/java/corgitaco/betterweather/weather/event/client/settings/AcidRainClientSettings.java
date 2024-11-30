package corgitaco.betterweather.weather.event.client.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;

import corgitaco.betterweather.weather.event.client.AcidRainClient;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

public class AcidRainClientSettings extends RainClientSettings {

    public static final Codec<AcidRainClientSettings> CODEC = RecordCodecBuilder.create(builder -> rainFields(builder)
            .and(Codec.BOOL.fieldOf("smokeParticles").forGetter(blizzardClientSettings -> blizzardClientSettings.addSmokeParticles))
            .apply(builder, AcidRainClientSettings::new));

//    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(RainClientSettings.VALUE_COMMENTS), (map) -> {
//        map.put("smokeParticles", "Do smoke particles appear on the ground?");
//    });

    public boolean addSmokeParticles = true;

    public AcidRainClientSettings(JsonObject json) {
        this(
                new ColorSettings(GsonHelper.getAsJsonObject(json, "colorSettings")),
                GsonHelper.getAsFloat(json, "skyOpacity"),
                GsonHelper.getAsFloat(json, "fogDensity"),
                GsonHelper.getAsBoolean(json, "sunsetSunriseColor"),
                LegacyWeatherRendering.valueOf(GsonHelper.getAsString(json, "renderingType")),
                new ResourceLocation(GsonHelper.getAsString(json, "rainTexture")),
                new ResourceLocation(GsonHelper.getAsString(json, "snowTexture")),
                GsonHelper.getAsBoolean(json, "addSmokeParticles")
        );
    }

    public AcidRainClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation rainTexture, ResourceLocation snowTexture, boolean addSmokeParticles) {
        this(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, LegacyWeatherRendering.ACIDIC, rainTexture, snowTexture, addSmokeParticles);
    }

    public AcidRainClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, LegacyWeatherRendering weatherRendering, ResourceLocation rainTexture, ResourceLocation snowTexture, boolean addSmokeParticles) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, weatherRendering, rainTexture, snowTexture);
        this.addSmokeParticles = addSmokeParticles;
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new AcidRainClient(this);
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.ACID_RAIN_CLIENT;
    }
}
