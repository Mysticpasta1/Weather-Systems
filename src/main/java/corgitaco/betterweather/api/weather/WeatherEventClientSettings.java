package corgitaco.betterweather.api.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.data.storage.WeatherEventSavedData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.Util;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraftforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class WeatherEventClientSettings {

    public static final Codec<WeatherEventClientSettings> CODEC = ExtraCodecs.lazyInitializedCodec(() -> BetterWeatherRegistry.CLIENT_WEATHER_EVENT_SETTINGS.get().getCodec()).dispatchStable(WeatherEventClientSettings::codec, Function.identity());

    private final ColorSettings colorSettings;
    private final float skyOpacity;
    private final float fogDensity;
    private final boolean sunsetSunriseColor;

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(ColorSettings.VALUE_COMMENTS), (map) -> {
        map.put("skyOpacity", "What is the opacity of the sky? 0.0 means hidden, 1.0 is fully visible.\n#Range 0.0-1.0");
        map.put("fogDensity", "How dense is fog?");
        map.put("sunsetSunriseColor", "Do sunsets/sunrises modify fog/sky color?");
    });

    public WeatherEventClientSettings(ColorSettings colorSettings, float skyOpacity, float fogDensity, boolean sunsetSunriseColor) {
        this.colorSettings = colorSettings;
        this.skyOpacity = skyOpacity;
        this.fogDensity = fogDensity;
        this.sunsetSunriseColor = sunsetSunriseColor;
    }

    public abstract Codec<? extends WeatherEventClientSettings> codec();

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
}
