package corgitaco.betterweather.weather.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.util.TomlCommentedConfigOps;
import corgitaco.betterweather.weather.event.client.settings.SnowClientSettings;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class Snow extends Weather {

    public static final Codec<Snow> CODEC = RecordCodecBuilder.<Snow>create(instance -> commonFields(instance)
            .and(Codec.INT.fieldOf("blockLightThreshold").forGetter(snow -> snow.blockLightThreshold))
    .and(Codec.INT.fieldOf("chunkTickChance").forGetter(snow -> snow.chunkTickChance))
    .and(ForgeRegistries.BLOCKS.getCodec().optionalFieldOf("snowBlock", Blocks.SNOW).forGetter(snow -> snow.snowBlock))
    .and(Codec.BOOL.fieldOf("snowLayering").forGetter(snow -> snow.snowLayers))
    .and(Codec.BOOL.fieldOf("waterFreezes").forGetter(snow -> snow.waterFreezes)).apply(instance, Snow::new));

//    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(Weather.VALUE_COMMENTS), (map) -> {
//        map.putAll(SnowClientSettings.VALUE_COMMENTS);
//        map.put("blockLightThreshold", "The max sky brightness to allow snow to generate.");
//        map.put("snowBlock", "What block generates when chunks are ticking? If this block has the layers property & \"snowLayering\" is true, this block will layer.");
//        map.put("snowLayering", "Does the \"snowBlock\" layer when chunks are ticking? Only works if the\"snowBlock\" has a layers property!");
//        map.put("waterFreezes", "Does water freeze?");
//        map.put("entityEffects", "Entity/Category(left) effect(s)(right).");
//        map.put("chunkTickChance", "The chance of a chunk being ticked for this tick.");
//    });
//
//    public static final TomlCommentedConfigOps CONFIG_OPS = new TomlCommentedConfigOps(VALUE_COMMENTS, true);

    private final int chunkTickChance;
    private final int blockLightThreshold;
    private final Block snowBlock;
    private final boolean snowLayers;
    private final boolean waterFreezes;

    public Snow(WeatherClientSettings clientSettings, BasicSettings basicSettings, DecaySettings decaySettings, int chunkTickChance, int blockLightThreshold, Block snowBlock, boolean snowLayers, boolean waterFreezes) {
        super(clientSettings, basicSettings, decaySettings);
        this.chunkTickChance = chunkTickChance;
        this.blockLightThreshold = blockLightThreshold;
        this.snowBlock = snowBlock;
        this.snowLayers = snowLayers;
        this.waterFreezes = waterFreezes;
    }

    @Override
    public void chunkTick(LevelChunk chunk, ServerLevel world) {
        if (this.chunkTickChance < 1) {
            return;
        }
        if (world.random.nextInt(chunkTickChance) == 0) {
            ChunkPos chunkpos = chunk.getPos();
            int xStart = chunkpos.getMinBlockX();
            int zStart = chunkpos.getMinBlockZ();
            BlockPos randomHeightMapPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, world.getBlockRandomPos(xStart, 0, zStart, 15));
            BlockPos randomPosDown = randomHeightMapPos.below();
            BlockState blockState = world.getBlockState(randomHeightMapPos);

            Holder<Biome> biome = world.getBiome(randomHeightMapPos);
            if (!isValidBiome(biome.unwrapKey().get().location())) {
                return;
            }

            if (waterFreezes) {
                if (biome.value().shouldFreeze(world, randomPosDown)) {
                    world.setBlockAndUpdate(randomPosDown, Blocks.ICE.defaultBlockState());
                }
            }

            if (meetsStateRequirements(world, randomHeightMapPos)) {
                world.setBlockAndUpdate(randomHeightMapPos, this.snowBlock.defaultBlockState());
                return;
            }

            if (this.snowLayers) {
                if (meetsLayeringRequirement(world, randomHeightMapPos)) {
                    int currentLayer = blockState.getValue(BlockStateProperties.LAYERS);

                    if (currentLayer < 7) {
                        world.setBlock(randomHeightMapPos, blockState.setValue(BlockStateProperties.LAYERS, currentLayer + 1), 2);
                    }
                }
            }
        }
    }

    private boolean meetsStateRequirements(LevelReader worldIn, BlockPos pos) {
        if (pos.getY() >= 0 && pos.getY() < worldIn.getMaxBuildHeight() && worldIn.getBrightness(LightLayer.BLOCK, pos) < this.blockLightThreshold) {
            BlockState blockstate = worldIn.getBlockState(pos);
            BlockState defaultState = this.snowBlock.defaultBlockState();
            return (blockstate.isAir() && defaultState.canSurvive(worldIn, pos));
        }

        return false;
    }

    private boolean meetsLayeringRequirement(LevelReader worldIn, BlockPos pos) {
        BlockState blockstate = worldIn.getBlockState(pos);
        BlockState defaultState = this.snowBlock.defaultBlockState();
        return (defaultState.hasProperty(BlockStateProperties.LAYERS) && this.snowLayers && blockstate.getBlock() == this.snowBlock);
    }


    @Override
    public WeatherType<Snow> type() {
        return Weather.WeatherType.SNOW;
    }
}
