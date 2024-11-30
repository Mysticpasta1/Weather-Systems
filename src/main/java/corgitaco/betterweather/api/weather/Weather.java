package corgitaco.betterweather.api.weather;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.BiomeCheck;
import corgitaco.betterweather.weather.event.EntityCheck;
import corgitaco.betterweather.weather.event.Snow;
import corgitaco.betterweather.weather.event.Sunny;
import corgitaco.betterweather.weather.event.client.settings.SunnyClientSettings;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.core.registries.BuiltInRegistries.*;

@Mod.EventBusSubscriber
public class Weather implements WeatherEventSettings {

    public static final Codec<Weather> CODEC = WeatherType.CODEC.dispatch(Weather::type, WeatherType::codec);

    public static <T extends Weather> Products.P3<RecordCodecBuilder.Mu<T>, WeatherClientSettings, BasicSettings, DecaySettings> commonFields(RecordCodecBuilder.Instance<T> builder) {
        return builder.group(
                        WeatherClientSettings.CODEC.fieldOf("client").forGetter(Weather::getClientSettings))
                .and(BasicSettings.CODEC.fieldOf("basic").forGetter(Weather::getBasicSettings))
                .and(DecaySettings.CODEC.optionalFieldOf("decay", DecaySettings.NONE).forGetter(Weather::getDecaySetting));
    }

    public static record Effect(MobEffect pEffect/*, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon*/) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(inst -> inst.group(BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(Effect::pEffect)/*, Codec.INT.fieldOf("duration").forGetter(Effect::pDuration), Codec.INT.fieldOf("amplifier").forGetter(Effect::pAmplifier), Codec.BOOL.fieldOf("ambient").forGetter(Effect::pAmbient), Codec.BOOL.fieldOf("visible").forGetter(Effect::pVisible), Codec.BOOL.fieldOf("showIcon").forGetter(Effect::pShowIcon)*/).apply(inst, Effect::new));

        public static Effect from(MobEffect effect) {
            var instance = new MobEffectInstance(effect);
            return new Effect(effect);
        }
    }

    public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE_CODEC = CompoundTag.CODEC.xmap(MobEffectInstance::load, mobEffectInstance -> mobEffectInstance.save(new CompoundTag()));

    public static final Codec<Weather> CODEC_IMPL = RecordCodecBuilder.create(instance -> commonFields(instance).apply(instance, Weather::new));

    public static final Logger LOGGER = LogManager.getLogger();


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

//    public Weather(JsonObject json) {
//        this(
//                WeatherClientSettings.fromJson(GsonHelper.getAsJsonObject(json, "clientSettings")),
//                new BasicSettings(GsonHelper.getAsJsonObject(json, "basicSettings")),
//                new DecaySettings(GsonHelper.getAsJsonObject(json, "decaySettings"))
//        );
//    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", WeatherType.REGISTRY.inverse().get(type()).toString());
//        json.addProperty("clientSettings", this.clientSettings.toJson());
//        json.addProperty("basicSettings", this.basicSettings.toJson());
//        json.addProperty("decaySettings", DecaySettings.CODEC.encodeStart(JsonOps.INSTANCE, this.decaySettings).getOrThrow(false, System.out::println));

        return json;
    }

    public final double getDefaultChance() {
        return basicSettings.defaultChance();
    }

    public void worldTick(ServerLevel world, int tickSpeed, long worldTime) {
    }

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
        return clientSettings;
    }

    @OnlyIn(Dist.CLIENT)
    public Weather setClientSettings(WeatherClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        return this;
    }

    public List<BiomeCheck> getBiomeCondition() {
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
        if (client != null) {
            return client;
        } else {
            return sunnyClientSettings.createClientSettings();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void initClient() {
        this.client = getClientSettings().createClientSettings();
    }

    public WeatherType<? extends Weather> type() {
        return WeatherType.BASIC;
    }

    @Override
    public double getTemperatureModifierAtPosition(BlockPos pos) {
        return getTemperatureOffsetRaw();
    }

    @Override
    public double getHumidityModifierAtPosition(BlockPos pos) {
        return getHumidityOffsetRaw();
    }

    public record BasicSettings(List<BiomeCheck> biomeCOndition, double defaultChance, double temperatureOffset,
                                double humidityOffset, boolean isThundering, int lightningChance,
                                Map<EntityCheck, List<Effect>> entityEffects, boolean showClouds) {
        public static final Codec<BasicSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BiomeCheck.ENTITY_CHECK_CODEC.listOf().fieldOf("biomeCondition").forGetter(BasicSettings::biomeCOndition),
                Codec.DOUBLE.fieldOf("defaultChance").forGetter(BasicSettings::defaultChance),
                Codec.DOUBLE.fieldOf("temperatureOffset").forGetter(BasicSettings::temperatureOffset),
                Codec.DOUBLE.fieldOf("humidityOffset").forGetter(BasicSettings::humidityOffset),
                Codec.BOOL.fieldOf("isThundering").forGetter(BasicSettings::isThundering),
                Codec.INT.fieldOf("lightningChance").forGetter(BasicSettings::lightningChance),
                Codec.unboundedMap(EntityCheck.ENTITY_CHECK_CODEC, Codec.list(Effect.CODEC)).fieldOf("entityEffects").forGetter(BasicSettings::entityEffects),
                Codec.BOOL.fieldOf("showClouds").forGetter(BasicSettings::showClouds)
        ).apply(instance, BasicSettings::new));

//        public BasicSettings(JsonObject json) {
//            this(
//                    json.getAsJsonArray("biomeCondition").asList().stream().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).map(BiomeCheck::fromJson).toList(),
//                    GsonHelper.getAsDouble(json, "defaultChance"),
//                    GsonHelper.getAsDouble(json, "temperatureOffset"),
//                    GsonHelper.getAsDouble(json, "humidityOffset"),
//                    GsonHelper.getAsBoolean(json, "isThundering"),
//                    GsonHelper.getAsInt(json, "lightningChance"),
//                    GsonHelper.getAsJsonObject(json, "entityEffects").entrySet().stream().map(entry -> {
//                        var key = EntityCheck.fromString(entry.getKey());
//
//                        var value = entry.getValue().getAsJsonArray().asList().stream().filter(a -> a.isJsonObject()).map(a -> a.getAsJsonObject()).map(a -> MOB_EFFECT_INSTANCE_CODEC.decode(JsonOps.INSTANCE, a).getOrThrow(false, System.out::println).getFirst()).toList();
//
//                        return new Pair<>(key, value);
//                    }).toList(),
//                    GsonHelper.getAsBoolean(json, "showClouds")
//            );
//        }

        private Object effectInstanceFromJson(JsonElement value) {
            return null;
        }

        public BasicSettings(List<BiomeCheck> biomeCondition, double defaultChance, double temperatureOffset, double humidityOffset, boolean isThundering, int lightningChance) {
            this(biomeCondition, defaultChance, temperatureOffset, humidityOffset, isThundering, lightningChance, Collections.emptyMap(), true);
        }
        public BasicSettings(List<BiomeCheck> biomeCondition, double defaultChance, double temperatureOffset, double humidityOffset, boolean isThundering, int lightningChance, Map<EntityCheck, List<Effect>> entityEffects) {
            this(biomeCondition, defaultChance, temperatureOffset, humidityOffset, isThundering, lightningChance, entityEffects, true);
        }
    }

    public record DecaySettings(int chunkTickChance, int entityDamageChance, Map<Block, Block> decayer,
                                Map<EntityCheck, Float> entityDamage) {
        public static final Codec<DecaySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("chunkTickChance").forGetter(DecaySettings::chunkTickChance),
                Codec.INT.fieldOf("entityDamageChance").forGetter(DecaySettings::entityDamageChance),
                Codec.unboundedMap(ForgeRegistries.BLOCKS.getCodec(), ForgeRegistries.BLOCKS.getCodec()).fieldOf("decayer").forGetter(DecaySettings::decayer),
                Codec.unboundedMap(EntityCheck.ENTITY_CHECK_CODEC, Codec.FLOAT).fieldOf("entityDamage").forGetter(DecaySettings::entityDamage)
        ).apply(instance, DecaySettings::new));
        public static final DecaySettings NONE = new DecaySettings(0, 0, Collections.emptyMap(), Collections.emptyMap());

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

                float damage = entityDamage.entrySet().stream().filter(a -> a.getKey().isValid(entity)).findFirst().map(Map.Entry::getValue).orElse(0f);

                if (damage == 0)
                    entity.hurt((new DamageSource(Holder.direct(new DamageType("generic", 0.0F)))), damage);
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

                    if (currentBlock != null) world.setBlockAndUpdate(randomPos, currentBlock);
                    if (currentBlockDown != null) world.setBlockAndUpdate(randomPosDown, currentBlockDown);
                }
            }
        }
    }

    public record WeatherType<T extends Weather>(Codec<T> codec) {
        public static final BiMap<ResourceLocation, WeatherType<?>> REGISTRY = HashBiMap.create();
        public static final Codec<WeatherType<?>> CODEC =  ResourceLocation.CODEC.xmap(REGISTRY::get, weatherType -> REGISTRY.inverse().get(weatherType));

        public static final WeatherType<Weather> BASIC = register("basic", Weather.CODEC_IMPL);
        public static final WeatherType<Snow> SNOW = register("snow", Snow.CODEC);
        public static final WeatherType<Sunny> SUNNY = register("sunny", Sunny.CODEC);

        public static <T extends Weather> WeatherType<T> register(String name, Codec<T> codec) {
            var type = new WeatherType<>(codec);

            REGISTRY.put(new ResourceLocation(BetterWeather.MOD_ID, name), type);
            return type;
        }

        public static void register() {
        }
    }
}