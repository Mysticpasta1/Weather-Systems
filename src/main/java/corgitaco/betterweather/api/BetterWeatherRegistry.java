package corgitaco.betterweather.api;

import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BetterWeatherRegistry {
    public static final Map<ResourceLocation, Weather> DEFAULT_EVENTS = new HashMap<>();

    public static void init() {
        Weather.WeatherType.register();
        WeatherClientSettings.WeatherClientSettingType.register();
    }

    public static Map<String, Weather> getWeather() {
        HashMap<String, Weather> weatherEventHashMap = new HashMap<>();
        for (ResourceLocation rl : DEFAULT_EVENTS.keySet().stream().toList()) {
            weatherEventHashMap.put(rl.getPath(), DEFAULT_EVENTS.get(rl));
        }
        return weatherEventHashMap;
    }
}
