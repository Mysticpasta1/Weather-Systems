package corgitaco.betterweather.api.weather;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherType;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.EntityCheck;
import corgitaco.betterweather.weather.event.client.settings.SunnyClientSettings;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;

@Mod.EventBusSubscriber
public class Weather implements WeatherEventSettings {
    public static <T extends Weather> Products.P3<RecordCodecBuilder.Mu<T>, WeatherClientSettings, BasicSettings, DecaySettings> commonFields(RecordCodecBuilder.Instance<T> builder) {
        return builder.group(
                WeatherClientSettings.CODEC.fieldOf("client").forGetter(Weather::getClientSettings))
                .and(BasicSettings.CODEC.fieldOf("basic").forGetter(Weather::getBasicSettings))
                .and(DecaySettings.CODEC.optionalFieldOf("decay", DecaySettings.NONE).forGetter(Weather::getDecaySetting));
    }

    public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE_CODEC = CompoundTag.CODEC.xmap(MobEffectInstance::load, mobEffectInstance -> mobEffectInstance.save(new CompoundTag()));

    public static final Codec<Weather> CODEC_IMPL = RecordCodecBuilder.create(instance -> commonFields(instance).apply(instance, Weather::new));

    public static final Logger LOGGER = LogManager.getLogger();


    public static final Codec<Weather> CODEC = WeatherType.CODEC;

    public static final Map<String, String> VALUE_COMMENTS = Util.make(new HashMap<>(WeatherClientSettings.VALUE_COMMENTS), (map) -> {
        map.put("defaultChance", "What is the default chance for this weather event to occur? This value is only used when Seasons are NOT present in the given dimension.");
        map.put("temperatureOffset", "What is the temperature offset for valid biomes?");
        map.put("humidityOffset", "What is the temperature offset for valid biomes?");
        map.put("isThundering", "Determines whether or not this weather event may spawn lightning and sets world info internally for MC and mods to use.");
        map.put("lightningChance", "How often does lightning spawn? Requires \"isThundering\" to be true.");
        map.put("type", "Target Weather Event's Registry ID to configure settings for in this config.");
        map.put("biomeCondition", "Better Weather uses a prefix system for what biomes weather is allowed to function in.\n Prefix Guide:\n \"#\" - Biome category representable.\n \"$\" - Biome dictionary representable.\n \",\" - Creates a new condition, separate from the previous.\n \"ALL\" - Spawn in all biomes(no condition).\n \"!\" - Negates/flips/does the reverse of the condition.\n \"\" - No prefix serves as a biome ID OR Mod ID representable.\n\n Here are a few examples:\n1. \"byg#THE_END, $OCEAN\" would mean that the ore may spawn in biomes with the name space \"byg\" AND in the \"END\" biome category, OR all biomes in the \"OCEAN\" dictionary.\n2. \"byg:guiana_shield, #MESA\" would mean that the ore may spawn in the \"byg:guiana_shield\" OR all biomes in the \"MESA\" category.\n3. \"byg#ICY$MOUNTAIN\" would mean that the ore may only spawn in biomes from byg in the \"ICY\" category and \"MOUNTAIN\" dictionary type.\n4. \"!byg#DESERT\" would mean that the ore may only spawn in biomes that are NOT from byg and NOT in the \"DESERT\" category.\n5. \"ALL\", spawn everywhere. \n6. \"\" Don't spawn anywhere.");
        map.put("entityDamageChance", "The chance of an entity getting damaged every tick when acid rain is on the player's position.");
        map.put("decayer", "What the specified block(left) \"decays\" into(right).");
        map.put("entityDamage", "Entity/Category(left) damage strength(right).");
        map.put("chunkTickChance", "The chance of a chunk being ticked for this tick.");
    });

    private final BasicSettings basicSettings;
    private final DecaySettings decaySettings;
    private final ReferenceArraySet<ResourceLocation> validBiomes = new ReferenceArraySet<>();
    private WeatherClientSettings clientSettings;
    private WeatherEventClient<?> client;
    private String name;

    private final SunnyClientSettings sunnyClientSettings = new SunnyClientSettings(new ColorSettings("0B8649", 3.0, "0B8649", 3.0));

    public Weather(WeatherClientSettings clientSettings, BasicSettings basicSettings, DecaySettings decaySettings) {
        this.clientSettings = clientSettings;
        this.basicSettings = basicSettings;
        this.decaySettings = decaySettings;
    }

    public final double getDefaultChance() {
        return basicSettings.defaultChance();
    }

    public void worldTick(ServerLevel world, int tickSpeed, long worldTime) {}

    public void livingEntityUpdate(Entity entity) {
    }

    /**
     * This is called in the chunk ticking iterator.
     */
    public void chunkTick(LevelChunk chunk, ServerLevel world) {
        if (world.random.nextInt(16) == 0) {
            ChunkPos chunkpos = chunk.getPos();
            int xStart = chunkpos.getMinBlockX();
            int zStart = chunkpos.getMinBlockZ();
            BlockPos randomPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, world.getBlockRandomPos(xStart, 0, zStart, 15));
            BlockPos randomPosDown = randomPos.below();

            Holder<Biome> biome = world.getBiome(randomPos);
            if (isValidBiome(biome.unwrapKey().get().location())) {
                if (spawnSnowInFreezingClimates() && biome.value().shouldFreeze(world, randomPosDown)) {
                    world.setBlockAndUpdate(randomPosDown, Blocks.ICE.defaultBlockState());
                }

                if (spawnSnowInFreezingClimates() && biome.value().shouldSnow(world, randomPos)) {
                    world.setBlockAndUpdate(randomPos, Blocks.SNOW.defaultBlockState());
                }

                if (world.isRainingAt(randomPos.above(25)) && fillBlocksWithWater()) {
                    world.getBlockState(randomPosDown).getBlock().handlePrecipitation(world.getBlockState(randomPosDown), world, randomPosDown, Biome.Precipitation.RAIN);
                }
            }
        }

        decaySettings.chunkTick(this::isValidBiome, chunk, world);
    }
    public final void doChunkTick(LevelChunk chunk, ServerLevel world) {
        chunkTick(chunk, world);
    }

    public void onChunkLoad(ChunkHolder chunk, ServerLevel world) {

    }

    public final void doChunkLoad(ChunkHolder chunk, ServerLevel world) {
        onChunkLoad(chunk, world);
    }

    public boolean fillBlocksWithWater() {
        return false;
    }

    public DecaySettings getDecaySetting() {
        return decaySettings;
    }

    public boolean spawnSnowInFreezingClimates() {
        return true;
    }

    public final Component successTranslationTextComponent(String key) {
        return Component.translatable("commands.bw.setweather.success", Component.translatable("bw.weather." + key));
    }

    public String getName() {
        return name;
    }

    public Weather setName(String name) {
        this.name = name;
        return this;
    }

    public void fillBiomes(Registry<Biome> biomeRegistry) {
        this.validBiomes.addAll(biomeRegistry.keySet());
    }

    public WeatherClientSettings getClientSettings() {
        return Objects.requireNonNullElse(clientSettings, sunnyClientSettings);
    }

    @OnlyIn(Dist.CLIENT)
    public Weather setClientSettings(WeatherClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        return this;
    }

    public String getBiomeCondition() {
        return basicSettings.biomeCOndition();
    }

    public boolean isValidBiome(ResourceLocation biome) {
        return this.validBiomes.contains(biome);
    }
    public boolean isValidBiome(ResourceKey<Biome> biome) {
        return isValidBiome(biome.location());
    }

    public double getTemperatureOffsetRaw() {
        return basicSettings.temperatureOffset;
    }

    public double getHumidityOffsetRaw() {
        return basicSettings.humidityOffset();
    }

    public boolean isThundering() {
        return basicSettings.isThundering();
    }

    public int getLightningChance() {
        return basicSettings.lightningChance();
    }

    public BasicSettings getBasicSettings() {
        return basicSettings;
    }

    @OnlyIn(Dist.CLIENT)
    public WeatherEventClient<?> getClient() {
        if(client != null) {
            return client;
        } else {
            return sunnyClientSettings.createClientSettings();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setClient(WeatherEventClient<?> client) {
        this.client = client;
    }

    public WeatherType<?> type() {
        return WeatherType.BASIC.get();
    }

    @Override
    public double getTemperatureModifierAtPosition(BlockPos pos) {
        return getTemperatureOffsetRaw();
    }

    @Override
    public double getHumidityModifierAtPosition(BlockPos pos) {
        return getHumidityOffsetRaw();
    }

    public record BasicSettings(String biomeCOndition, double defaultChance, double temperatureOffset, double humidityOffset, boolean isThundering, int lightningChance, List<Pair<EntityCheck, List<MobEffectInstance>>> entityEffects) {
        public static final Codec<BasicSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("biomeCondition").forGetter(BasicSettings::biomeCOndition),
                Codec.DOUBLE.fieldOf("defaultChance").forGetter(BasicSettings::defaultChance),
                Codec.DOUBLE.fieldOf("temperatureOffset").forGetter(BasicSettings::temperatureOffset),
                Codec.DOUBLE.fieldOf("humidityOffset").forGetter(BasicSettings::humidityOffset),
                Codec.BOOL.fieldOf("isThundering").forGetter(BasicSettings::isThundering),
                Codec.INT.fieldOf("lightningChance").forGetter(BasicSettings::lightningChance),
                Codec.list(Codec.pair(EntityCheck.ENTITY_CHECK_CODEC, Codec.list(MOB_EFFECT_INSTANCE_CODEC))).fieldOf("entityEffects").forGetter(BasicSettings::entityEffects)
        ).apply(instance, BasicSettings::new));

        public BasicSettings(String biomeCOndition, double defaultChance, double temperatureOffset, double humidityOffset, boolean isThundering, int lightningChance) {
            this(biomeCOndition, defaultChance, temperatureOffset, humidityOffset, isThundering, lightningChance, Collections.emptyList());
        }
    }

    public record DecaySettings(int chunkTickChance, int entityDamageChance, Map<Block, Block> decayer, List<Pair<EntityCheck, Float>> entityDamage) {
        public static final Codec<DecaySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("chunkTickChance").forGetter(DecaySettings::chunkTickChance),
                Codec.INT.fieldOf("entityDamageChance").forGetter(DecaySettings::entityDamageChance),
                Codec.unboundedMap(ForgeRegistries.BLOCKS.getCodec(), ForgeRegistries.BLOCKS.getCodec()).fieldOf("decayer").forGetter(DecaySettings::decayer),
                Codec.pair(EntityCheck.ENTITY_CHECK_CODEC, Codec.FLOAT).listOf().fieldOf("entityDamage").forGetter(DecaySettings::entityDamage)
        ).apply(instance, DecaySettings::new));
        public static final DecaySettings NONE = new DecaySettings(0, 0, Collections.emptyMap(), Collections.emptyList());

        private BlockState checkBlockState(BlockState blockState) {
            var block = decayer.getOrDefault(blockState.getBlock(), null);

            return block != null ? block.defaultBlockState() : null;
        }

        public void livingEntityUpdate(Predicate<ResourceLocation> biomeCheck, LivingEntity entity) {
            if (chunkTickChance < 1) {
                return;
            }

            Level world = entity.level();
            if (world.random.nextInt(entityDamageChance) == 0) {
                BlockPos entityPosition = entity.blockPosition();
                Holder<Biome> biome = world.getBiome(entityPosition);
                if (world.getHeight(Heightmap.Types.MOTION_BLOCKING, entityPosition.getX(), entityPosition.getZ()) > entityPosition.getY() || !biomeCheck.test(biome.unwrapKey().get().location()) || biome.value().shouldSnow(world, entityPosition)) {
                    return;
                }

                var damage = entityDamage.stream().filter(a -> a.getFirst().isValid(entity)).findFirst().map(a -> a.getSecond()).orElse(0f);

                if(damage == 0) entity.hurt((new DamageSource(Holder.direct(new DamageType("generic", 0.0F)))), damage);
            }
    }

        public void chunkTick(Predicate<ResourceLocation> biomeCheck, LevelChunk chunk, ServerLevel world) {
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

                if (biomeCheck.test(biome.unwrapKey().get().location()) && !biome.value().shouldSnow(world, randomPos)) {
                    BlockState currentBlock = checkBlockState(world.getBlockState(randomPos));
                    BlockState currentBlockDown = checkBlockState(world.getBlockState(randomPosDown));

                    if(currentBlock != null) world.setBlockAndUpdate(randomPos, currentBlock);
                    if(currentBlockDown != null) world.setBlockAndUpdate(randomPosDown, currentBlockDown);
                }
            }
        }
        }
    }