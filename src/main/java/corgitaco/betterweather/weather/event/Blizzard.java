package corgitaco.betterweather.weather.event;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.api.weather.WeatherEventClientSettings;
import corgitaco.betterweather.core.SoundRegistry;
import corgitaco.betterweather.util.TomlCommentedConfigOps;
import corgitaco.betterweather.weather.event.client.settings.BlizzardClientSettings;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class Blizzard extends WeatherEvent {

    public static final Codec<Blizzard> CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(WeatherEventClientSettings.CODEC.fieldOf("clientSettings").forGetter((blizzard) -> {
            return blizzard.getClientSettings();
        }), Codec.STRING.fieldOf("biomeCondition").forGetter(blizzard -> {
            return blizzard.getBiomeCondition();
        }), Codec.DOUBLE.fieldOf("temperatureOffset").forGetter(blizzard -> {
            return blizzard.getTemperatureOffsetRaw();
        }), Codec.DOUBLE.fieldOf("humidityOffset").forGetter(blizzard -> {
            return blizzard.getHumidityOffsetRaw();
        }), Codec.DOUBLE.fieldOf("defaultChance").forGetter(blizzard -> {
            return blizzard.getDefaultChance();
        }), Codec.INT.fieldOf("blockLightThreshold").forGetter(blizzard -> {
            return blizzard.blockLightThreshold;
        }), Codec.INT.fieldOf("chunkTickChance").forGetter(blizzard -> {
            return blizzard.chunkTickChance;
        }), ResourceLocation.CODEC.fieldOf("snowBlock").forGetter(blizzard -> {
            return ForgeRegistries.BLOCKS.getKey(blizzard.snowBlock);
        }), Codec.BOOL.fieldOf("snowLayering").forGetter(blizzard -> {
            return blizzard.snowLayers;
        }), Codec.BOOL.fieldOf("waterFreezes").forGetter(blizzard -> {
            return blizzard.waterFreezes;
        }), Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).fieldOf("entityEffects").forGetter(blizzard -> {
            return blizzard.entityOrCategoryToEffectsMap;
        }), Codec.BOOL.fieldOf("isThundering").forGetter(rain -> {
            return rain.isThundering();
        }), Codec.INT.fieldOf("lightningChance").forGetter(rain -> {
            return rain.getLightningChance();
        })).apply(builder, (clientSettings, biomeCondition, temperatureOffsetRaw, humidityOffsetRaw, defaultChance, tickRate, blockLightThreshold, snowBlockID, snowLayers, waterFreezes, entityOrCategoryToEffectsMap, isThundering, lightningChance) -> {
            Optional<Block> blockOptional = Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(snowBlockID));
            if (!blockOptional.isPresent()) {
                BetterWeather.LOGGER.error("\"" + snowBlockID.toString() + "\" is not a valid block ID in the registry, defaulting to \"minecraft:snow\"...");
            }
            return new Blizzard(clientSettings, biomeCondition, defaultChance, temperatureOffsetRaw, humidityOffsetRaw, tickRate, blockLightThreshold, blockOptional.orElse(Blocks.SNOW), snowLayers, waterFreezes, entityOrCategoryToEffectsMap, isThundering, lightningChance);
        });
    });

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherEvent.VALUE_COMMENTS), (map) -> {
        map.putAll(BlizzardClientSettings.VALUE_COMMENTS);
        map.put("blockLightThreshold", "The max sky brightness to allow snow to generate.");
        map.put("snowBlock", "What block generates when chunks are ticking? If this block has the layers property & \"snowLayering\" is true, this block will layer.");
        map.put("snowLayering", "Does the \"snowBlock\" layer when chunks are ticking? Only works if the\"snowBlock\" has a layers property!");
        map.put("waterFreezes", "Does water freeze?");
        map.put("entityEffects", "Entity/Category(left) effect(s)(right).");
        map.put("chunkTickChance", "The chance of a chunk being ticked for this tick.");
    });

    public static final TomlCommentedConfigOps CONFIG_OPS = new TomlCommentedConfigOps(VALUE_COMMENTS, true);

    public static final Blizzard DEFAULT = new Blizzard(new BlizzardClientSettings(new ColorSettings(Integer.MAX_VALUE, 0.0, Integer.MAX_VALUE, 0.0), 0.0F, 0.2F, false, Rain.SNOW_LOCATION, Holder.direct(SoundRegistry.BLIZZARD_LOOP2), 0.6F, 0.6F), Rain.DEFAULT_BIOME_CONDITION, 0.1D, !MODIFY_TEMPERATURE ? 0.0 : -0.5, 0.1, 2, 10, Blocks.SNOW, true, true, Util.make(new HashMap<>(), ((stringListHashMap) -> stringListHashMap.put(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER).toString(), ImmutableList.of(Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(MobEffects.MOVEMENT_SLOWDOWN)).toString())))), false, 0);

    public static final Blizzard DEFAULT_THUNDERING = new Blizzard(new BlizzardClientSettings(new ColorSettings(Integer.MAX_VALUE, 0.0, Integer.MAX_VALUE, 0.0), 0.0F, 0.2F, false, Rain.SNOW_LOCATION, Holder.direct(SoundRegistry.BLIZZARD_LOOP2), 0.6F, 0.6F), Rain.DEFAULT_BIOME_CONDITION, 0.05D, !MODIFY_TEMPERATURE ? 0.0 : -0.5, 0.1, 2, 10, Blocks.SNOW, true, true, Util.make(new HashMap<>(), ((stringListHashMap) -> stringListHashMap.put(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER).toString(), ImmutableList.of(Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(MobEffects.MOVEMENT_SLOWDOWN)).toString())))), true, 100000);


    public static final Map<MobCategory, List<EntityType<?>>> CLASSIFICATION_ENTITY_TYPES = Util.make(new EnumMap<>(MobCategory.class), (map) -> {
        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES) {
            map.computeIfAbsent(entityType.getCategory(), (mobCategory -> new ArrayList<>())).add(entityType);
        }
    });

    private final int chunkTickChance;
    private final int blockLightThreshold;
    private final Block snowBlock;
    private final boolean snowLayers;
    private final boolean waterFreezes;
    private final Map<String, List<String>> entityOrCategoryToEffectsMap;
    private final Map<EntityType<?>, List<MobEffectInstance>> entityTypeToEffectMap = new Reference2ReferenceArrayMap<>();


    public Blizzard(WeatherEventClientSettings clientSettings, String biomeCondition, double defaultChance, double temperatureOffsetRaw, double humidityOffsetRaw, int chunkTickChance, int blockLightThreshold, Block snowBlock, boolean snowLayers, boolean waterFreezes, Map<String, List<String>> entityOrCategoryToEffectsMap, boolean isThundering, int lightningChance) {
        super(clientSettings, biomeCondition, defaultChance, temperatureOffsetRaw, humidityOffsetRaw, isThundering, lightningChance);
        this.chunkTickChance = chunkTickChance;
        this.blockLightThreshold = blockLightThreshold;
        this.snowBlock = snowBlock;
        this.snowLayers = snowLayers;
        this.waterFreezes = waterFreezes;
        this.entityOrCategoryToEffectsMap = entityOrCategoryToEffectsMap;

        for (Map.Entry<String, List<String>> entry : entityOrCategoryToEffectsMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (key.startsWith("category/")) {
                String mobCategory = key.substring("category/".length()).toUpperCase();

                MobCategory[] values = MobCategory.values();
                if (Arrays.stream(values).noneMatch(difficulty -> difficulty.toString().equals(mobCategory))) {
                    BetterWeather.LOGGER.error("\"" + mobCategory + "\" is not a valid mob category value. Skipping mob category entry...\nValid Mob Categories: " + Arrays.toString(values));
                    continue;
                }

                for (EntityType<?> entityType : CLASSIFICATION_ENTITY_TYPES.get(MobCategory.valueOf(mobCategory))) {
                    addEntry(value, entityType);
                }
                continue;
            }

            ResourceLocation entityTypeID = tryParse(key.toLowerCase());
            if (entityTypeID != null && !ForgeRegistries.ENTITY_TYPES.containsKey(entityTypeID)) {
                BetterWeather.LOGGER.error("\"" + key + "\" is not a valid entity ID. Skipping entry...");
                continue;
            }
            addEntry(value, ForgeRegistries.ENTITY_TYPES.getValue(entityTypeID));
        }
    }

    private void addEntry(List<String> value, EntityType<?> entityType) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (String effectArguments : value) {
            MobEffectInstance effectInstanceFromString = createEffectInstanceFromString(effectArguments);
            if (effectInstanceFromString != null) {
                effects.add(effectInstanceFromString);
            }
        }


        this.entityTypeToEffectMap.put(entityType, effects);
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
            if (!isValidBiome(biome.value())) {
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
    public boolean spawnSnowInFreezingClimates() {
        return true;
    }

    @Override
    public void worldTick(ServerLevel world, int tickSpeed, long worldTime) {}

    @Override
    public Codec<? extends WeatherEvent> codec() {
        return CODEC;
    }

    @Override
    public DynamicOps<?> configOps() {
        return CONFIG_OPS;
    }

    @Override
    public double getTemperatureModifierAtPosition(BlockPos pos) {
        return getTemperatureOffsetRaw();
    }

    @Override
    public double getHumidityModifierAtPosition(BlockPos pos) {
        return getHumidityOffsetRaw();
    }

    @Nullable
    private static MobEffectInstance createEffectInstanceFromString(String effectString) {
        String[] split = effectString.split("(?=[\\$])");

        MobEffect effect = null;
        int amplifier = 4;

        for (int i = 0; i < split.length; i++) {
            String variable = split[i];
            if (i == 0) {
                if (!(variable.startsWith("$") || variable.startsWith("#"))) {
                    ResourceLocation resourceLocation = tryParse(variable);
                    if (resourceLocation != null) {
                        if (ForgeRegistries.MOB_EFFECTS.containsKey(resourceLocation)) {
                            effect = ForgeRegistries.MOB_EFFECTS.getValue(resourceLocation);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    if (variable.startsWith("$")) {
                        String substring = variable.substring(1);
                        try {
                            if (!substring.isEmpty()) {
                                amplifier = Integer.getInteger(substring);
                            }
                        } catch (NumberFormatException e) {
                            BetterWeather.LOGGER.error("Not a number: " + substring);
                        }
                    }
                }
            }
        }
        return effect != null ? new MobEffectInstance(effect, 5, amplifier, true, false, false) : null;
    }

    @Nullable
    public static ResourceLocation tryParse(String id) {
        try {
            return new ResourceLocation(id);
        } catch (ResourceLocationException resourcelocationexception) {
            BetterWeather.LOGGER.error(resourcelocationexception.getMessage());
            return null;
        }
    }

}
