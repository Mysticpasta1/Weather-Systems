package corgitaco.betterweather;

import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.config.BetterWeatherClientConfig;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.weather.WeatherLoader;
import corgitaco.betterweather.weather.event.*;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
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
        BetterWeatherRegistry.init();
        ReloadListenerRegistry.register(PackType.SERVER_DATA, WeatherLoader.getInstance());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        BetterWeatherConfig.serialize();

//        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "sunny"), DefaultEvents.SUNNY);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "partly_cloudy"), DefaultEvents.PARTLY_CLOUDY);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_drizzle"), DefaultEvents.ACID_DRIZZLE);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "dusting"), DefaultEvents.DUSTING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "drizzle"), DefaultEvents.DRIZZLE);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "cloudy"), DefaultEvents.CLOUDY);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_rain"), DefaultEvents.ACID_RAIN);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "snow"), DefaultEvents.SNOW);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "rain"), DefaultEvents.RAIN);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "overcast"), DefaultEvents.OVERCAST);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_downpour"), DefaultEvents.ACID_DOWNPOUR);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "blizzard"), DefaultEvents.BLIZZARD);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "downpour"), DefaultEvents.DOWNPOUR);

        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "heat_lightning"), DefaultEvents.HEAT_LIGHTNING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "acid_lightning"), DefaultEvents.ACID_LIGHTNING);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "thunder_blizzard"), DefaultEvents.THUNDER_BLIZZARD);
        BetterWeatherRegistry.DEFAULT_EVENTS.put(new ResourceLocation(BetterWeather.MOD_ID, "lightning"), DefaultEvents.LIGHTNING);
    }

    private void lateSetup(FMLLoadCompleteEvent event) {

    }
}
