package corgitaco.betterweather;

import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.config.BetterWeatherClientConfig;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.weather.event.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
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
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        BetterWeatherConfig.serialize();

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "clear"), DefaultEvents.DEFAULT_SUNNY);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_rain"), DefaultEvents.ACID_RAIN_DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "snow"), DefaultEvents.SNOW_DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "cloudy"), DefaultEvents.CLOUDY_DEFAULT);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "rain"), DefaultEvents.RAIN_DEFAULT);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_rain_thundering"), DefaultEvents.ACID_RAIN_DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "blizzard"), DefaultEvents.SNOW_DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "cloudy_thundering"), DefaultEvents.CLOUDY_DEFAULT_THUNDERING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "lightning"), DefaultEvents.RAIN_DEFAULT_THUNDERING);
    }

    private void lateSetup(FMLLoadCompleteEvent event) {

    }
}
