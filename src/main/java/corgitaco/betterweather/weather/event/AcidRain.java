package corgitaco.betterweather.weather.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.api.weather.WeatherEventClientSettings;
import corgitaco.betterweather.util.BetterWeatherUtil;
import corgitaco.betterweather.util.TomlCommentedConfigOps;
import corgitaco.betterweather.weather.event.client.settings.AcidRainClientSettings;
import corgitaco.betterweather.weather.event.client.settings.RainClientSettings;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class AcidRain extends Rain {

    public static final Codec<AcidRain> CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(WeatherEventClientSettings.CODEC.fieldOf("clientSettings").forGetter((rain) -> {
            return rain.getClientSettings();
        }), Codec.STRING.fieldOf("biomeCondition").forGetter(rain -> {
            return rain.getBiomeCondition();
        }), Codec.DOUBLE.fieldOf("defaultChance").forGetter(rain -> {
            return rain.getDefaultChance();
        }), Codec.DOUBLE.fieldOf("temperatureOffset").forGetter(rain -> {
            return rain.getTemperatureOffsetRaw();
        }), Codec.DOUBLE.fieldOf("humidityOffset").forGetter(rain -> {
            return rain.getHumidityOffsetRaw();
        }), Codec.INT.fieldOf("chunkTickChance").forGetter(blizzard -> {
            return blizzard.chunkTickChance;
        }), Codec.INT.fieldOf("entityDamageChance").forGetter(blizzard -> {
            return blizzard.entityDamageChance;
        }), Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("decayer").forGetter(rain -> {
            return BetterWeatherUtil.transformBlockBlocksToResourceLocations(rain.blockToBlock);
        }), Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("entityDamage").forGetter(rain -> {
            return rain.entityDamageSerializable;
        }), Codec.BOOL.fieldOf("isThundering").forGetter(rain -> {
            return rain.isThundering();
        }), Codec.INT.fieldOf("lightningChance").forGetter(rain -> {
            return rain.getLightningChance();
        })).apply(builder, AcidRain::new);
    });

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherEvent.VALUE_COMMENTS), (map) -> {
        map.putAll(RainClientSettings.VALUE_COMMENTS);
        map.put("entityDamageChance", "The chance of an entity getting damaged every tick when acid rain is on the player's position.");
        map.put("decayer", "What the specified block(left) \"decays\" into(right).");
        map.put("entityDamage", "Entity/Category(left) damage strength(right).");
        map.put("chunkTickChance", "The chance of a chunk being ticked for this tick.");
    });

    public static final TomlCommentedConfigOps CONFIG_OPS = new TomlCommentedConfigOps(VALUE_COMMENTS, true);

    public static final ResourceLocation ACID_RAIN_LOCATION = new ResourceLocation(BetterWeather.MOD_ID, "textures/environment/acid_rain.png");

    public static final IdentityHashMap<ResourceLocation, ResourceLocation> DEFAULT_DECAYER = Util.make(new IdentityHashMap<>(), (map) -> {
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block.defaultBlockState().is(BlockTags.LEAVES) || block.defaultBlockState().is(BlockTags.CROPS)) {
                map.put(ForgeRegistries.BLOCKS.getKey(block), (ForgeRegistries.BLOCKS.getKey(Blocks.AIR)));
            }
        }
        map.put(ForgeRegistries.BLOCKS.getKey(Blocks.GRASS_BLOCK), ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        map.put(ForgeRegistries.BLOCKS.getKey(Blocks.PODZOL), ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        map.put(ForgeRegistries.BLOCKS.getKey(Blocks.MYCELIUM), ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    });

    public static final HashMap<String, Float> DEFAULT_ENTITY_DAMAGE = Util.make(new HashMap<>(), (map) -> {
        map.put("category/monster", 0.5F);
        map.put("minecraft:player", 0.5F);
    });

    public static final AcidRain DEFAULT = new AcidRain(new AcidRainClientSettings(RAIN_COLORS, 0.0F, -1.0F, true, ACID_RAIN_LOCATION, SNOW_LOCATION, true), DEFAULT_BIOME_CONDITION, 0.25D, !MODIFY_TEMPERATURE ? 0.0 : -0.1, 0.1, 150, 100, AcidRain.DEFAULT_DECAYER, AcidRain.DEFAULT_ENTITY_DAMAGE, false, 0);

    public static final AcidRain DEFAULT_THUNDERING = new AcidRain(new AcidRainClientSettings(THUNDER_COLORS, 0.0F, -1.0F, true, ACID_RAIN_LOCATION, SNOW_LOCATION, true), DEFAULT_BIOME_CONDITION, 0.125D, !MODIFY_TEMPERATURE ? 0.0 : -0.1, 0.1, 150, 100, DEFAULT_DECAYER, DEFAULT_ENTITY_DAMAGE, true, 100000);

    private final int chunkTickChance;
    private final int entityDamageChance;
    private final IdentityHashMap<Block, Block> blockToBlock;
    private final Map<String, Float> entityDamageSerializable;
    private final Object2FloatArrayMap<EntityType<?>> entityDamage = new Object2FloatArrayMap<>();


    public AcidRain(WeatherEventClientSettings clientSettings, String biomeCondition, double defaultChance, double temperatureOffsetRaw, double humidityOffsetRaw, int chunkTickChance, int entityDamageChance, Map<ResourceLocation, ResourceLocation> blockToBlock, Map<String, Float> entityDamage, boolean isThundering, int lightningChance) {
        super(clientSettings, biomeCondition, defaultChance, temperatureOffsetRaw, humidityOffsetRaw, isThundering, lightningChance);
        this.chunkTickChance = chunkTickChance;
        this.entityDamageChance = entityDamageChance;
        this.blockToBlock = BetterWeatherUtil.transformBlockBlockResourceLocations(blockToBlock);
        this.entityDamageSerializable = entityDamage;

        for (Map.Entry<String, Float> entry : entityDamage.entrySet()) {
            String key = entry.getKey();
            float value = entry.getValue();
            if (key.startsWith("category/")) {
                String mobCategory = key.substring("category/".length()).toUpperCase();

                MobCategory[] values = MobCategory.values();
                if (Arrays.stream(values).noneMatch(difficulty -> difficulty.toString().equals(mobCategory))) {
                    BetterWeather.LOGGER.error("\"" + mobCategory + "\" is not a valid mob category value. Skipping mob category entry...\nValid Mob Categories: " + Arrays.toString(values));
                    continue;
                }

                for (EntityType<?> entityType : Blizzard.CLASSIFICATION_ENTITY_TYPES.get(MobCategory.valueOf(mobCategory))) {
                    this.entityDamage.put(entityType, value);
                }
                continue;
            }

            ResourceLocation entityTypeID = Blizzard.tryParse(key.toLowerCase());
            if (entityTypeID != null && !ForgeRegistries.ENTITY_TYPES.containsKey(entityTypeID)) {
                BetterWeather.LOGGER.error("\"" + key + "\" is not a valid entity ID. Skipping entry...");
                continue;
            }
            this.entityDamage.put(ForgeRegistries.ENTITY_TYPES.getValue(entityTypeID), value);
        }
    }

    @Override
    public void chunkTick(LevelChunk chunk, ServerLevel world) {
        super.chunkTick(chunk, world);
        if (this.chunkTickChance < 1) {
            return;
        }
        if (world.random.nextInt(chunkTickChance) == 0) {
            ChunkPos chunkpos = chunk.getPos();
            int xStart = chunkpos.getMinBlockX();
            int zStart = chunkpos.getMinBlockZ();
            BlockPos randomPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, world.getBlockRandomPos(xStart, 0, zStart, 15));
            BlockPos randomPosDown = randomPos.below();
            Holder<Biome> biome = world.getBiome(randomPos);

            if (isValidBiome(biome.value()) && !biome.value().shouldSnow(world, randomPos)) {
                Block currentBlock = world.getBlockState(randomPos).getBlock();
                Block currentBlockDown = world.getBlockState(randomPosDown).getBlock();

                if (this.blockToBlock.containsKey(currentBlock)) {
                    world.setBlockAndUpdate(randomPos, this.blockToBlock.get(currentBlock).defaultBlockState());
                }
                if (this.blockToBlock.containsKey(currentBlockDown)) {
                    world.setBlockAndUpdate(randomPosDown, this.blockToBlock.get(currentBlockDown).defaultBlockState());
                }
            }
        }
    }

    @SubscribeEvent
    public void livingEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (this.chunkTickChance < 1) {
            return;
        }

        LivingEntity entity = event.getEntity();

        Level world = entity.level();
        if (world.random.nextInt(entityDamageChance) == 0) {
            BlockPos entityPosition = entity.blockPosition();
            Holder<Biome> biome = world.getBiome(entityPosition);
            if (world.getHeight(Heightmap.Types.MOTION_BLOCKING, entityPosition.getX(), entityPosition.getZ()) > entityPosition.getY() || !isValidBiome(biome.value()) || biome.value().shouldSnow(world, entityPosition)) {
                return;
            }

            if (this.entityDamage.containsKey(entity.getType())) {
                entity.hurt((new DamageSource(Holder.direct(new DamageType("generic", 0.0F)))), this.entityDamage.getFloat(entity.getType()));
            }
        }
    }

    @Override
    public Codec<? extends WeatherEvent> codec() {
        return CODEC;
    }

    @Override
    public DynamicOps<?> configOps() {
        return CONFIG_OPS;
    }
}
