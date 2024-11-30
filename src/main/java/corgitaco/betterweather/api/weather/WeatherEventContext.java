package corgitaco.betterweather.api.weather;

import corgitaco.betterweather.weather.BWWeatherEventContext;
import corgitaco.betterweather.weather.event.Sunny;
import net.minecraft.resources.ResourceLocation;

public interface WeatherEventContext {

    static Weather getWeather(BWWeatherEventContext weatherContext) {
        var weather = weatherContext.getCurrentEvent();
        if(weather != null) {
            return weather;
        } else return Sunny.INSTANCE;
    }

    boolean isLocalizedWeather();

    ResourceLocation getCurrentWeatherEventKey();

    Weather getCurrentWeatherEntry();

    WeatherEventSettings getCurrentWeatherEventSettings();
}
