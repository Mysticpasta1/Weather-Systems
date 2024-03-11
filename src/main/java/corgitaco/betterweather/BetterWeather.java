package corgitaco.betterweather;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.api.weather.WeatherEventClientSettings;
import corgitaco.betterweather.config.BetterWeatherClientConfig;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.data.network.NetworkHandler;
import corgitaco.betterweather.weather.event.*;
import corgitaco.betterweather.weather.event.client.settings.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.LazyLoadedValue;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Mod("betterweather")
public class BetterWeather {
    public static final String MOD_ID = "betterweather";
    public static final Path CONFIG_PATH = new File(String.valueOf(FMLPaths.CONFIGDIR.get().resolve(MOD_ID))).toPath();
    public static final Logger LOGGER = LogManager.getLogger();

    public static final BetterWeatherClientConfig CLIENT_CONFIG = new BetterWeatherClientConfig();

    public BetterWeather() {
        if (!CONFIG_PATH.toFile().exists())
            CONFIG_PATH.toFile().mkdir();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        bus.addListener(this::lateSetup);
        BetterWeatherRegistry.init(bus);
        WEATHER_CLIENT_EVENT.register(bus);
        WEATHER_EVENT.register(bus);
    }

    public static final DeferredRegister<Codec<? extends WeatherEvent>> WEATHER_EVENT = DeferredRegister.create(BetterWeatherRegistry.WEATHER_EVENT_KEY, MOD_ID);
    public static final DeferredRegister<Codec<? extends WeatherEventClientSettings>> WEATHER_CLIENT_EVENT = DeferredRegister.create(BetterWeatherRegistry.CLIENT_WEATHER_EVENT_KEY, MOD_ID);

    public static final RegistryObject<Codec<? extends  WeatherEventClientSettings>> ACID_RAIN_CLIENT = WEATHER_CLIENT_EVENT.register("acid_rain", () -> AcidRainClientSettings.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEventClientSettings>> BLIZZARD_CLIENT =  WEATHER_CLIENT_EVENT.register("blizzard", () -> BlizzardClientSettings.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEventClientSettings>> CLOUDY_CLIENT = WEATHER_CLIENT_EVENT.register("cloudy", () -> CloudyClientSettings.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEventClientSettings>> NONE_CLIENT = WEATHER_CLIENT_EVENT.register("none", () -> NoneClientSettings.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEventClientSettings>> RAIN_CLIENT = WEATHER_CLIENT_EVENT.register("rain", () -> RainClientSettings.CODEC);

    public static final RegistryObject<Codec<? extends  WeatherEvent>> ACID_RAIN = WEATHER_EVENT.register("acid_rain",  () -> AcidRain.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEvent>> BLIZZARD = WEATHER_EVENT.register("blizzard",  () -> Blizzard.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEvent>> CLOUDY = WEATHER_EVENT.register("cloudy",  () -> Cloudy.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEvent>> NONE = WEATHER_EVENT.register("none",  () -> None.CODEC);
    public static final RegistryObject<Codec<? extends  WeatherEvent>> RAIN = WEATHER_EVENT.register("rain",  () -> Rain.CODEC);
    private void commonSetup(FMLCommonSetupEvent event) {
        BetterWeatherConfig.serialize();

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "none"), None.DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_rain"), AcidRain.DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "blizzard"), Blizzard.DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "cloudy"), Cloudy.DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "rain"), Rain.DEFAULT);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_rain_thundering"), AcidRain.DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_thundering"), Blizzard.DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "cloudy_thundering"), Cloudy.DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "thundering"), Rain.DEFAULT_THUNDERING);
    }

    private void lateSetup(FMLLoadCompleteEvent event) {

    }
}
