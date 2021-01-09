package corgitaco.betterweather;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import corgitaco.betterweather.config.BetterWeatherConfig;
import corgitaco.betterweather.config.BetterWeatherConfigClient;
import corgitaco.betterweather.config.json.SeasonConfig;
import corgitaco.betterweather.config.json.overrides.BiomeOverrideJsonHandler;
import corgitaco.betterweather.datastorage.BetterWeatherData;
import corgitaco.betterweather.datastorage.BetterWeatherSeasonData;
import corgitaco.betterweather.datastorage.network.NetworkHandler;
import corgitaco.betterweather.season.BWSeasonSystem;
import corgitaco.betterweather.season.Season;
import corgitaco.betterweather.server.ConfigReloadCommand;
import corgitaco.betterweather.server.SetSeasonCommand;
import corgitaco.betterweather.server.SetWeatherCommand;
import corgitaco.betterweather.weatherevent.BWWeatherEventSystem;
import corgitaco.betterweather.weatherevent.weatherevents.AcidRain;
import corgitaco.betterweather.weatherevent.weatherevents.Blizzard;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;

@Mod("betterweather")
public class BetterWeather {
    public static Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "betterweather";
    public static int SEASON_LENGTH = 240000;
    public static int SEASON_CYCLE_LENGTH = SEASON_LENGTH * 4;

    public static final Path CONFIG_PATH = new File(String.valueOf(FMLPaths.CONFIGDIR.get().resolve(MOD_ID))).toPath();

    public static Registry<Biome> biomeRegistryEarlyAccess;

    public BetterWeather() {
        File dir = new File(CONFIG_PATH.toString());
        if (!dir.exists())
            dir.mkdir();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        BetterWeatherConfig.loadConfig(CONFIG_PATH.resolve(MOD_ID + "-common.toml"));
        BetterWeatherConfigClient.loadConfig(CONFIG_PATH.resolve(MOD_ID + "-client.toml"));
    }

    public static BetterWeatherSeasonData seasonData = null;
    public static BetterWeatherData weatherData = null;


    public void commonSetup(FMLCommonSetupEvent event) {
//        GlobalEntityTypeAttributes.put(BWEntityRegistry.TORNADO, TornadoEntity.setCustomAttributes().create());
        BetterWeatherConfig.handleCommonConfig();
        NetworkHandler.init();
    }


    public void clientSetup(FMLClientSetupEvent event) {
//        RenderingRegistry.registerEntityRenderingHandler(BWEntityRegistry.TORNADO, TornadoRenderer::new);
    }

    public static void loadClientConfigs() {
        BetterWeatherConfigClient.loadConfig(CONFIG_PATH.resolve(MOD_ID + "-client.toml"));
        loadCommonConfigs();
    }

    public static void loadCommonConfigs() {
        SeasonConfig.handleBWSeasonsConfig(BetterWeather.CONFIG_PATH.resolve(BetterWeather.MOD_ID + "-seasons.json"));

        Season.SUB_SEASON_MAP.forEach((subSeasonName, subSeason) -> {
            Path overrideFilePath = CONFIG_PATH.resolve("overrides").resolve(subSeasonName + "-override.json");
            if (subSeason.getParentSeason() == BWSeasonSystem.SeasonVal.WINTER)
                BiomeOverrideJsonHandler.handleOverrideJsonConfigs(overrideFilePath, Season.SubSeason.WINTER_OVERRIDE, subSeason);
            else
                BiomeOverrideJsonHandler.handleOverrideJsonConfigs(overrideFilePath, new IdentityHashMap<>(), subSeason);
        });
    }


    public enum WeatherEvent {
        NONE,
        ACID_RAIN,
        BLIZZARD,
//        HAIL,
//        HEATWAVE,
//        WINDSTORM,
//        SANDSTORM,
    }


    @Mod.EventBusSubscriber(modid = BetterWeather.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BetterWeatherEvents {
        @SubscribeEvent
        public static void worldTick(TickEvent.WorldTickEvent event) {
            setWeatherData(event.world);
            setSeasonData(event.world);

            if (event.phase == TickEvent.Phase.END) {
                if (event.side.isServer()) {
                    ServerWorld serverWorld = (ServerWorld) event.world;
                    if (serverWorld.getDimensionKey() == World.OVERWORLD) {
                        World world = event.world;
                        int tickSpeed = world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
                        long worldTime = world.getWorldInfo().getGameTime();

                        BWSeasonSystem.updateSeasonTime();

                        BWSeasonSystem.updateSeasonPacket(serverWorld.getPlayers(), world, false);
                        BWWeatherEventSystem.updateWeatherEventPacket(serverWorld.getPlayers(), world, false);

                        if (weatherData.getEventValue() == WeatherEvent.ACID_RAIN) {
                            AcidRain.modifyLiveWorldForAcidRain(serverWorld, tickSpeed, worldTime, (serverWorld.getChunkProvider()).chunkManager.getLoadedChunksIterable());
                        } else if (weatherData.getEventValue() == WeatherEvent.BLIZZARD) {
                            Blizzard.modifyLiveWorldForBlizzard(serverWorld, tickSpeed, worldTime, (serverWorld.getChunkProvider()).chunkManager.getLoadedChunksIterable());
                        } else if (weatherData.getEventValue() == WeatherEvent.NONE && BetterWeatherConfig.decaySnowAndIce.get())
                            Blizzard.decayIceAndSnowFaster(serverWorld, worldTime, (serverWorld.getChunkProvider()).chunkManager.getLoadedChunksIterable());
                    }
                }
            }
        }

        @SubscribeEvent
        public static void renderTickEvent(TickEvent.RenderTickEvent event) {

        }

        @SubscribeEvent
        public static void worldLoadEvent(WorldEvent.Load event) {
        }

        @SubscribeEvent
        public static void playerTickEvent(TickEvent.PlayerTickEvent event) {
            setWeatherData(event.player.world);
        }

        @SubscribeEvent
        public static void entityTickEvent(LivingEvent.LivingUpdateEvent event) {
            AcidRain.entityHandler(event.getEntity());
            Blizzard.blizzardEntityHandler(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
            BWSeasonSystem.updateSeasonPacket(Collections.singletonList((ServerPlayerEntity) event.getPlayer()), event.getPlayer().world, true);
            BWWeatherEventSystem.updateWeatherEventPacket(Collections.singletonList((ServerPlayerEntity) event.getPlayer()), event.getPlayer().world, true);
        }


        @SubscribeEvent
        public static void clientTickEvent(TickEvent.ClientTickEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.phase == TickEvent.Phase.START) {
                if (minecraft.world != null && minecraft.player != null) {
                    if (minecraft.world.getDimensionKey() == World.OVERWORLD) {
                        if (minecraft.world.getWorldInfo().getGameTime() % 10 == 0) {
                            BWSeasonSystem.clientSeason();
                        }

                        AcidRain.handleRainTexture(minecraft);


                        Blizzard.handleBlizzardRenderDistance(minecraft);
                        Blizzard.blizzardSoundHandler(minecraft, minecraft.gameRenderer.getActiveRenderInfo());
                    }
                }
            }
        }

        @SubscribeEvent
        public static void commandRegisterEvent(FMLServerStartingEvent event) {
            BetterWeather.LOGGER.debug("BW: \"Server Starting\" Event Starting...");
            register(event.getServer().getCommandManager().getDispatcher());
            BetterWeather.LOGGER.info("BW: \"Server Starting\" Event Complete!");
        }
    }


    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LOGGER.debug("Registering Better Weather commands...");
        LiteralCommandNode<CommandSource> source = dispatcher.register(
                Commands.literal(MOD_ID).requires(commandSource -> commandSource.hasPermissionLevel(3))
                        .then(SetSeasonCommand.register(dispatcher))
                        .then(SetWeatherCommand.register(dispatcher))
                        .then(ConfigReloadCommand.register(dispatcher))

        );
        dispatcher.register(Commands.literal(MOD_ID).redirect(source));
        LOGGER.debug("Registered Better Weather Commands!");
    }

    public static void setSeasonData(IWorld world) {
        if (seasonData == null)
            seasonData = BetterWeatherSeasonData.get(world);
    }

    public static void setWeatherData(IWorld world) {
        if (weatherData == null)
            weatherData = BetterWeatherData.get(world);
    }


    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BetterWeatherClient {

        @SubscribeEvent
        public static void renderFogEvent(EntityViewRenderEvent.FogDensity event) {
            Minecraft minecraft = Minecraft.getInstance();
            Blizzard.handleFog(event, minecraft);
        }

        @SubscribeEvent
        public static void renderGameOverlayEventText(RenderGameOverlayEvent.Text event) {
            if (Minecraft.getInstance().gameSettings.showDebugInfo) {
                event.getLeft().add("Season: " + WordUtils.capitalize(BWSeasonSystem.cachedSeason.toString().toLowerCase()) + " | " + WordUtils.capitalize(BWSeasonSystem.cachedSubSeason.toString().replace("_", "").replace(BWSeasonSystem.cachedSeason.toString(), "").toLowerCase()));
            }
        }

        @SubscribeEvent
        public static void loggedInEvent(ClientPlayerNetworkEvent.LoggedInEvent event) {
            loadClientConfigs();
        }
    }
}
