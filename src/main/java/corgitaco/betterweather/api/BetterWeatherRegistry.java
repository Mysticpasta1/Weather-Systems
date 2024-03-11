package corgitaco.betterweather.api;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.api.weather.WeatherEventClientSettings;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static corgitaco.betterweather.BetterWeather.MOD_ID;

public class BetterWeatherRegistry {
    // TODO: Make this a registry similar to those of world gen registries.
    public static final Map<ResourceLocation, WeatherEvent> DEFAULT_EVENTS = new HashMap<>();

    public static final ResourceKey<Registry<Codec<? extends WeatherEvent>>> WEATHER_EVENT_KEY = ResourceKey.createRegistryKey(new ResourceLocation(MOD_ID, "weather_event"));

    public static final ResourceKey<Registry<Codec<? extends WeatherEventClientSettings>>> CLIENT_WEATHER_EVENT_KEY = ResourceKey.createRegistryKey(new ResourceLocation(MOD_ID, "weather_event_client"));

    public static final DeferredRegister<Codec<? extends WeatherEvent>> WEATHER_EVENT_CODEC = DeferredRegister.create(WEATHER_EVENT_KEY, MOD_ID);
    public static final DeferredRegister<Codec<? extends WeatherEventClientSettings>> WEATHER_CLIENT_EVENT_CODEC = DeferredRegister.create(CLIENT_WEATHER_EVENT_KEY, MOD_ID);

    public static final Supplier<RegistryBuilder<Codec<? extends WeatherEvent>>> WEATHER_CODEC_BUILDER_FACTORY =
            () -> new RegistryBuilder<Codec<? extends WeatherEvent>>().allowModification();
    public static final Supplier<RegistryBuilder<Codec<? extends WeatherEventClientSettings>>> WEATHER_CLIENT_CODEC_BUILDER_FACTORY =
            () -> new RegistryBuilder<Codec<? extends WeatherEventClientSettings>>().allowModification();


    public static final Supplier<IForgeRegistry<Codec<? extends WeatherEvent>>> WEATHER_EVENT = WEATHER_EVENT_CODEC.makeRegistry(WEATHER_CODEC_BUILDER_FACTORY);
    public static final Supplier<IForgeRegistry<Codec<? extends WeatherEventClientSettings>>> CLIENT_WEATHER_EVENT_SETTINGS = WEATHER_CLIENT_EVENT_CODEC.makeRegistry(WEATHER_CLIENT_CODEC_BUILDER_FACTORY);

    public static void init(IEventBus bus) {
        WEATHER_EVENT_CODEC.register(bus);
        WEATHER_CLIENT_EVENT_CODEC.register(bus);
    }

    public static Map<String, WeatherEvent> getWeather() {
        HashMap<String, WeatherEvent> weatherEventHashMap = new HashMap<>();
        for (ResourceLocation rl : DEFAULT_EVENTS.keySet().stream().toList()) {
            weatherEventHashMap.put(rl.getPath(), DEFAULT_EVENTS.get(rl));
        }
        return weatherEventHashMap;
    }
}
