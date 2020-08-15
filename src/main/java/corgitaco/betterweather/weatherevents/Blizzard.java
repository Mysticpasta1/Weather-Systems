package corgitaco.betterweather.weatherevents;

import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.SoundRegistry;
import corgitaco.betterweather.config.BetterWeatherConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class Blizzard {

    public static void blizzardEvent(Chunk chunk, World world, int tickSpeed, long worldTime) {
        ChunkPos chunkpos = chunk.getPos();
        int chunkXStart = chunkpos.getXStart();
        int chunkZStart = chunkpos.getZStart();
        IProfiler iprofiler = world.getProfiler();
        iprofiler.startSection("blizzard");
        BlockPos blockpos = world.getHeight(Heightmap.Type.MOTION_BLOCKING, world.getBlockRandomPos(chunkXStart, 0, chunkZStart, 15));
        Biome biome = world.getBiome(blockpos);
        if (world.isAreaLoaded(blockpos, 1)) {
            if (BetterWeather.BetterWeatherEvents.weatherData.isBlizzard() && world.getWorldInfo().isRaining() && worldTime % BetterWeatherConfig.tickSnowAndIcePlaceSpeed.get() == 0 && biome.getCategory() != Biome.Category.NETHER && biome.getCategory() != Biome.Category.THEEND && biome.getCategory() != Biome.Category.NONE && doBlizzardsAffectDeserts(biome) && BetterWeatherConfig.spawnSnowAndIce.get()) {
                if (world.getBlockState(blockpos.down()).getBlock() == Blocks.WATER || world.getBlockState(blockpos.down()).getFluidState().getLevel() == 8) {
                    world.setBlockState(blockpos.down(), Blocks.ICE.getDefaultState());
                }
                if (world.getBlockState(blockpos.down()).getMaterial() != Material.WATER && world.getBlockState(blockpos.down()).getMaterial() != Material.LAVA && world.getBlockState(blockpos.down()).getMaterial() != Material.ICE && world.getBlockState(blockpos.down()).getMaterial() != Material.CACTUS) {
                    if (world.getBlockState(blockpos).getBlock() != Blocks.SNOW)
                        world.setBlockState(blockpos, Blocks.SNOW.getDefaultState());

                    Block block = world.getBlockState(blockpos).getBlock();

                    if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 1 && world.rand.nextInt(5) == 2)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 2));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 2 && world.rand.nextInt(5) == 3)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 3));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 3 && world.rand.nextInt(5) == 0)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 4));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 4 && world.rand.nextInt(5) == 4)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 5));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 5 && world.rand.nextInt(5) == 0)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 6));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 6 && world.rand.nextInt(5) == 1)
                        world.setBlockState(blockpos, block.getDefaultState().with(BlockStateProperties.LAYERS_1_8, 7));
                    else if (block == Blocks.SNOW && world.getBlockState(blockpos).get(BlockStateProperties.LAYERS_1_8) == 7 && world.rand.nextInt(5) == 0)
                        world.setBlockState(blockpos, Blocks.SNOW_BLOCK.getDefaultState());
                }
            }
        }
        iprofiler.endSection();
    }

    static int cycleBlizzardSounds = 0;

    @OnlyIn(Dist.CLIENT)
    public static void playWeatherSounds(Minecraft mc, ActiveRenderInfo activeRenderInfo) {
        double volume = BetterWeatherConfig.blizzardVolume.get();
        double pitch = BetterWeatherConfig.blizzardPitch.get();
        BlockPos pos = new BlockPos(activeRenderInfo.getProjectedView());
        SimpleSound simplesound = new SimpleSound(SoundRegistry.BLIZZARD, SoundCategory.WEATHER, (float) volume, (float) pitch, pos.getX(), pos.getY(), pos.getZ());
        if (mc.world != null && mc.world.getWorldInfo().isRaining() && BetterWeather.BetterWeatherEvents.weatherData.isBlizzard()) {
            if (cycleBlizzardSounds == 0 || mc.world.getWorldInfo().getGameTime() % 2400 == 0) {
                mc.getSoundHandler().play(simplesound);
                cycleBlizzardSounds++;
            }
        }
        if (mc.world != null && !mc.world.getWorldInfo().isRaining()) {
            mc.getSoundHandler().stop(simplesound.getSoundLocation(), SoundCategory.WEATHER);
            if (cycleBlizzardSounds != 0)
                cycleBlizzardSounds = 0;
        }
    }

    public static void blizzardEntityHandler(Entity entity) {
        if (entity instanceof LivingEntity) {
            if (entity.world.getWorldInfo().isRaining() && BetterWeather.BetterWeatherEvents.weatherData.isBlizzard() && BetterWeatherConfig.doBlizzardsSlowPlayers.get())
                ((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 5, BetterWeatherConfig.blizzardSlownessAmplifier.get(), true, false));
        }
    }


    public static boolean doBlizzardsAffectDeserts(Biome biome) {
        if (!BetterWeatherConfig.doBlizzardsOccurInDeserts.get())
            return biome.getCategory() != Biome.Category.DESERT;
        else
            return true;
    }
}
