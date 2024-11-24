package corgitaco.betterweather.weather.event.client.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherClientSettingType;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherEventAudio;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.SnowClient;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

public class SnowClientSettings extends WeatherClientSettings implements WeatherEventAudio {

    public static final Codec<SnowClientSettings> CODEC = RecordCodecBuilder.create((builder) -> commonFields(builder)
            .and(ResourceLocation.CODEC.fieldOf("rendererTexture").forGetter(snowClientSettings -> snowClientSettings.textureLocation))
            .and(SoundEvent.CODEC.fieldOf("audioLocation").forGetter(snowClientSettings -> snowClientSettings.audio))
            .and(Codec.FLOAT.fieldOf("audioVolume").forGetter(snowClientSettings -> snowClientSettings.audioVolume))
            .and(Codec.FLOAT.fieldOf("audioPitch").forGetter(snowClientSettings -> snowClientSettings.audioPitch))
            .apply(builder, SnowClientSettings::new));

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

    public SnowClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation textureLocation, Holder<SoundEvent> audio, float audioVolume, float audioPitch) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor);
        this.textureLocation = textureLocation;
        this.audio = audio;
        this.audioVolume = audioVolume;
        this.audioPitch = audioPitch;
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new SnowClient(this);
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
        return WeatherClientSettingType.SNOW_CLIENT.get();
    }
}
