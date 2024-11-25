package corgitaco.betterweather.api.weather;

import net.minecraft.resources.ResourceLocation;

public interface WeatherEventContext {

    boolean isLocalizedWeather();

    ResourceLocation getCurrentWeatherEventKey();

    WeatherEventSettings getCurrentWeatherEventSettings();
}
