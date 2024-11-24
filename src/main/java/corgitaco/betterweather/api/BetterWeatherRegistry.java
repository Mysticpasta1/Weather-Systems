package corgitaco.betterweather.api;

import corgitaco.betterweather.WeatherClientSettingType;
import corgitaco.betterweather.WeatherType;
import corgitaco.betterweather.api.weather.Weather;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.HashMap;
import java.util.Map;

public class BetterWeatherRegistry {
    public static final Map<ResourceLocation, Weather> DEFAULT_EVENTS = new HashMap<>();

    public static void init(IEventBus bus) {
        WeatherType.register(bus);
        WeatherClientSettingType.register(bus);
    }

    public static Map<String, Weather> getWeather() {
        HashMap<String, Weather> weatherEventHashMap = new HashMap<>();
        for (ResourceLocation rl : DEFAULT_EVENTS.keySet().stream().toList()) {
            weatherEventHashMap.put(rl.getPath(), DEFAULT_EVENTS.get(rl));
        }
        return weatherEventHashMap;
    }
}
