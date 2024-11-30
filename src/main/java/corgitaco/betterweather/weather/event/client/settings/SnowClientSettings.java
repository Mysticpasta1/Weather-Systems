package corgitaco.betterweather.weather.event.client.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.api.weather.WeatherEventAudio;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.core.SoundRegistry;
import corgitaco.betterweather.weather.event.client.SnowClient;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

public class SnowClientSettings extends WeatherClientSettings implements WeatherEventAudio {

    public static final Codec<SnowClientSettings> CODEC = RecordCodecBuilder.create((builder) -> commonFields(builder)
            .and(ResourceLocation.CODEC.fieldOf("rendererTexture").forGetter(snowClientSettings -> snowClientSettings.textureLocation))
            .and(SoundSetting.CODEC.fieldOf("soundSetting").forGetter(snowClientSettings -> snowClientSettings.soundSettings))
            .apply(builder, SnowClientSettings::new));

    public final ResourceLocation textureLocation;
    private final SoundSetting soundSettings;

    public SnowClientSettings(JsonObject json) {
        this(
                new ColorSettings(GsonHelper.getAsJsonObject(json, "colorSettings")),
                GsonHelper.getAsFloat(json, "skyOpacity"),
                GsonHelper.getAsFloat(json, "fogDensity"),
                GsonHelper.getAsBoolean(json, "sunsetSunriseColor"),
                LegacyWeatherRendering.valueOf(GsonHelper.getAsString(json, "renderingType")),
                new ResourceLocation(GsonHelper.getAsString(json, "textureLocation")),
                SoundSetting.fromJson(json, "soundSettings")
        );
    }

    public SnowClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, ResourceLocation textureLocation, SoundSetting soundSettings) {
        this(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, LegacyWeatherRendering.SNOWY, textureLocation, soundSettings);
    }

    public SnowClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor, LegacyWeatherRendering weatherRendering, ResourceLocation textureLocation, SoundSetting soundSettings) {
        super(colorSettings, skyOpacity, fogDensity, sunsetSunriseColor, weatherRendering);
        this.textureLocation = textureLocation;
        this.soundSettings = soundSettings;
    }

    @Override
    public WeatherEventClient<?> createClientSettings() {
        return new SnowClient(this);
    }

    @Override
    public float getVolume() {
        return soundSettings.audioVolume;
    }

    @Override
    public float getPitch() {
        return soundSettings.audioPitch;
    }

    @Override
    public SoundEvent getSound() {
        return soundSettings.audio.value();
    }

    @Override
    public WeatherClientSettingType<?> type() {
        return WeatherClientSettingType.SNOW_CLIENT;
    }

    public record SoundSetting(float audioVolume, float audioPitch, Holder<SoundEvent> audio) {
        public static final Codec<SoundSetting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("audioVolume").forGetter(SoundSetting::audioVolume),
                Codec.FLOAT.fieldOf("audioPitch").forGetter(SoundSetting::audioPitch),
                SoundEvent.CODEC.fieldOf("audio").forGetter(SoundSetting::audio)
        ).apply(instance, SoundSetting::new));
        public static final SoundSetting EMPTY = new SoundSetting(0.0F, 0.0F, Holder.direct(SoundEvents.EMPTY));
        public static final SoundSetting BLIZZARD = new SoundSetting(0.6F, 0.6F, Holder.direct(SoundRegistry.BLIZZARD_LOOP2));

        public static SoundSetting fromJson(JsonObject json, String name) {
            var soundJson = json.getAsJsonObject(name);

            var audioVolume = GsonHelper.getAsFloat(soundJson, "audioVolume");
            var audioPitch = GsonHelper.getAsFloat(soundJson, "audioPitch");
            var audio = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceKey.create(Registries.SOUND_EVENT, new ResourceLocation(GsonHelper.getAsString(soundJson, "audio")))).orElse(BuiltInRegistries.SOUND_EVENT.createIntrusiveHolder(SoundEvents.EMPTY));

            return new SoundSetting(audioVolume, audioPitch, audio);
        }
    }
}
