package corgitaco.betterweather.weather.event.client.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherEventAudio;
import corgitaco.betterweather.api.weather.WeatherEventClientSettings;
import corgitaco.betterweather.weather.event.client.BlizzardClient;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

public class BlizzardClientSettings extends WeatherEventClientSettings implements WeatherEventAudio {

    public static final Codec<BlizzardClientSettings> CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(ColorSettings.CODEC.fieldOf("colorSettings").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.getColorSettings();
        }), Codec.FLOAT.fieldOf("skyOpacity").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.skyOpacity();
        }), Codec.FLOAT.fieldOf("fogDensity").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.fogDensity();
        }), Codec.BOOL.fieldOf("sunsetSunriseColor").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.sunsetSunriseColor();
        }), ResourceLocation.CODEC.fieldOf("rendererTexture").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.textureLocation;
        }), SoundEvent.CODEC.fieldOf("audioLocation").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.audio;
        }), Codec.FLOAT.fieldOf("audioVolume").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.audioVolume;
        }), Codec.FLOAT.fieldOf("audioPitch").forGetter(blizzardClientSettings -> {
            return blizzardClientSettings.audioPitch;
        })).apply(builder, BlizzardClientSettings::new);
    });

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherEventClientSettings.VALUE_COMMENTS), (map) -> {
        map.put("rendererTexture", "The texture used by the weather renderer.");
        map.put("audioLocation", "The audio played by the weather.");
        map.put("audioVolume", "The volume of the audio played by the weather.");
        map.put("audioPitch", "The pitch of the audio played by the weather.");
    });

    public final ResourceLocation textureLocation;
    private final SoundEvent audio;
    private final float audioVolume;
    private final float audioPitch;

    public BlizzardClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation textureLocation, SoundEvent audio, float audioVolume, float audioPitch) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor);
        this.textureLocation = textureLocation;
        this.audio = audio;
        this.audioVolume = audioVolume;
        this.audioPitch = audioPitch;
    }

    @Override
    public Codec<? extends WeatherEventClientSettings> codec() {
        return CODEC;
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new BlizzardClient(this);
    }

    @Override
    public float getVolume() {
        return this.audioVolume;
    }

    @Override
    public float getPitch() {
        return this.audioPitch;
    }

    @Override
    public SoundEvent getSound() {
        return this.audio;
    }
}
