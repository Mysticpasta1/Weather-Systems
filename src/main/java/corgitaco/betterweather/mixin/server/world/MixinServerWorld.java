package corgitaco.betterweather.mixin.server.world;

import corgitaco.betterweather.api.Climate;
import corgitaco.betterweather.api.weather.WeatherEvent;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.helpers.ServerBiomeUpdate;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;


@Mixin(ServerLevel.class)
public abstract class MixinServerWorld implements BetterWeatherWorldData, Climate {

    @Nullable
    private BWWeatherEventContext weatherContext;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void storeUpgradablePerWorldRegistry(MinecraftServer mc, Executor pDispatcher, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, ServerLevelData pServerLevelData, ResourceKey resourceKey, LevelStem pLevelStem, ChunkProgressListener pProgressListener, boolean pIsDebug, long pBiomeZoomSeed, List pCustomSpawners, boolean pTickTime, RandomSequences pRandomSequences, CallbackInfo ci) {
        if (mc.getLevel(resourceKey) != null) {
            new ServerBiomeUpdate(Objects.requireNonNull(mc.getLevel(resourceKey)).getChunkSource(), mc.registryAccess(), this.weatherContext).updateBiomeData();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateSkyBrightness()V"))
    private void tick(BooleanSupplier p_8794_, CallbackInfo ci) {
        if (weatherContext != null) {
            this.weatherContext.tick((ServerLevel) (Object) this);
        }
    }

    @Inject(method = "setWeatherParameters", at = @At("HEAD"))
    private void setWeatherForced(int clearWeatherTime, int weatherTime, boolean rain, boolean thunder, CallbackInfo ci) {
        if (this.weatherContext != null) {
            this.weatherContext.setWeatherForced(true);
        }
    }

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void tickLiveChunks(LevelChunk chunkIn, int p_8716_, CallbackInfo ci) {
        if (weatherContext != null) {
            weatherContext.getCurrentEvent().doChunkTick(chunkIn, (ServerLevel) (Object) this);
            doLightning((ServerLevel) (Object) this, chunkIn.getPos());
        }
    }

    private void doLightning(ServerLevel world, ChunkPos chunkpos) {
        if (weatherContext == null) {
            return;
        }

        int xStart = chunkpos.getMinBlockX();
        int zStart = chunkpos.getMinBlockZ();
        WeatherEvent currentEvent = weatherContext.getCurrentEvent();
        if (currentEvent.isThundering() && world.random.nextInt(currentEvent.getLightningChance()) == 0) {
            BlockPos blockpos = BlockPos.of(world.getLightEmission(world.getBlockRandomPos(xStart, 0, zStart, 15)));
            Holder<Biome> biome = world.getBiome(blockpos);
            if (currentEvent.isValidBiome(biome.value())) {
                DifficultyInstance difficultyinstance = world.getCurrentDifficultyAt(blockpos);
                boolean flag1 = world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && world.random.nextDouble() < (double) difficultyinstance.getEffectiveDifficulty() * 0.01D;
                if (flag1) {
                    SkeletonHorse skeletonhorseentity = EntityType.SKELETON_HORSE.create(world);
                    skeletonhorseentity.setTrap(true);
                    skeletonhorseentity.setAge(0);
                    skeletonhorseentity.setPos((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ());
                    world.addFreshEntity(skeletonhorseentity);
                }

                LightningBolt lightningboltentity = EntityType.LIGHTNING_BOLT.create(world);
                lightningboltentity.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                lightningboltentity.setVisualOnly(flag1);
                world.addFreshEntity(lightningboltentity);
            }
        }
    }


    @Redirect(method = "tickChunk", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;random:Lnet/minecraft/util/RandomSource;", ordinal = 0))
    private RandomSource neverSpawnLightning(ServerLevel instance) {
        return instance.getRandom();
    }

    @Nullable
    @Override
    public BWWeatherEventContext getWeatherEventContext() {
        return this.weatherContext;
    }

    @Nullable
    @Override
    public BWWeatherEventContext setWeatherEventContext(BWWeatherEventContext weatherEventContext) {
        this.weatherContext = weatherEventContext;
        return this.weatherContext;
    }
}
