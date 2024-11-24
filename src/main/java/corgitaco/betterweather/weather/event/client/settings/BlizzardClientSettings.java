package corgitaco.betterweather.weather.event.client.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherClientSettingType;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherEventAudio;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.BlizzardClient;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

public class BlizzardClientSettings extends WeatherClientSettings implements WeatherEventAudio {

    public static final Codec<BlizzardClientSettings> CODEC = RecordCodecBuilder.create((builder) -> commonFields(builder)
            .and(ResourceLocation.CODEC.fieldOf("rendererTexture").forGetter(blizzardClientSettings -> blizzardClientSettings.textureLocation))
            .and(SoundEvent.CODEC.fieldOf("audioLocation").forGetter(blizzardClientSettings -> blizzardClientSettings.audio))
            .and(Codec.FLOAT.fieldOf("audioVolume").forGetter(blizzardClientSettings -> blizzardClientSettings.audioVolume))
            .and(Codec.FLOAT.fieldOf("audioPitch").forGetter(blizzardClientSettings -> blizzardClientSettings.audioPitch))
            .apply(builder, BlizzardClientSettings::new));

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherClientSettings.VALUE_COMMENTS), (map) -> {
        map.put("rendererTexture", "The texture used by the weather renderer.");
        map.put("audioLocation", "The audio played by the weather.");
        map.put("audioVolume", "The volume of the audio played by the weather.");
        map.put("audioPitch", "The pitch of the audio played by the weather.");
    });

    public final ResourceLocation textureLocation;
    private final Holder<SoundEvent> audio;
    private final float audioVolume;
    private final float audioPitch;

    public BlizzardClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation textureLocation, Holder<SoundEvent> audio, float audioVolume, float audioPitch) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor);
        this.textureLocation = textureLocation;
        this.audio = audio;
        this.audioVolume = audioVolume;
        this.audioPitch = audioPitch;
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
        return this.audio.value();
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.BLIZZARD_CLIENT.get();
    }
}
