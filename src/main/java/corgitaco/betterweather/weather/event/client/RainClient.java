package corgitaco.betterweather.weather.event.client;

import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.client.settings.RainClientSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Random;
import java.util.function.Predicate;

public class RainClient extends WeatherEventClient<RainClientSettings> {

    protected final ResourceLocation rainTexture;
    protected final ResourceLocation snowTexture;
    private int rainSoundTime;

    public RainClient(RainClientSettings clientSettings) {
        super(clientSettings);
        this.rainTexture = clientSettings.rainTexture;
        this.snowTexture = clientSettings.snowTexture;
    }

    public void weatherParticlesAndSound(Camera renderInfo, Minecraft mc, float ticks, Predicate<ResourceKey<Biome>> validBiomes) {
        float particleStrength = mc.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
        if (!(particleStrength <= 0.0F)) {
            Random random = new Random((long) ticks * 312987231L);
            LevelReader worldReader = mc.level;
            BlockPos blockpos = new BlockPos(new Vec3i((int) renderInfo.getPosition().x, (int) renderInfo.getPosition().y, (int) renderInfo.getPosition().z));
            BlockPos blockpos1 = null;
            int particleCount = (int)(100.0F * particleStrength * particleStrength) / (mc.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

            for(int particleCounter = 0; particleCounter < particleCount; ++particleCounter) {
                int randomAddX = random.nextInt(21) - 10;
                int randomAddZ = random.nextInt(21) - 10;
                BlockPos motionBlockingHeightMinus1 = worldReader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(randomAddX, 0, randomAddZ)).below();
                Holder<Biome> biome = worldReader.getBiome(motionBlockingHeightMinus1);
                if (!validBiomes.test(biome.unwrapKey().get())) {
                    continue;
                }

                if (motionBlockingHeightMinus1.getY() > 0 && motionBlockingHeightMinus1.getY() <= blockpos.getY() + 10 && motionBlockingHeightMinus1.getY() >= blockpos.getY() - 10 && biome.value().getPrecipitationAt(blockpos) == Biome.Precipitation.RAIN && biome.value().getBaseTemperature() >= 0.15F) {
                    blockpos1 = motionBlockingHeightMinus1;
                    if (mc.options.particles().get() == ParticleStatus.MINIMAL) {
                        break;
                    }

                    double randDouble = random.nextDouble();
                    double randDouble2 = random.nextDouble();
                    BlockState blockstate = worldReader.getBlockState(motionBlockingHeightMinus1);
                    FluidState fluidstate = worldReader.getFluidState(motionBlockingHeightMinus1);
                    VoxelShape voxelshape = blockstate.getCollisionShape(worldReader, motionBlockingHeightMinus1);
                    double voxelShapeMax = voxelshape.max(Direction.Axis.Y, randDouble, randDouble2);
                    double fluidstateActualHeight = fluidstate.getHeight(worldReader, motionBlockingHeightMinus1);
                    double particleMaxAddedY = Math.max(voxelShapeMax, fluidstateActualHeight);
                    addParticlesToWorld(mc, motionBlockingHeightMinus1, randDouble, randDouble2, blockstate, fluidstate, particleMaxAddedY);
                }
            }

            if (blockpos1 != null && random.nextInt(3) < this.rainSoundTime++) {
                this.rainSoundTime = 0;
                if (blockpos1.getY() > blockpos.getY() + 1 && worldReader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
                    mc.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                } else {
                    mc.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                }
            }
        }
    }

    protected void addParticlesToWorld(Minecraft mc, BlockPos motionBlockingHeightMinus1, double randDouble, double randDouble2, BlockState blockstate, FluidState fluidstate, double particleMaxAddedY) {
        SimpleParticleType iparticledata = !fluidstate.is(FluidTags.LAVA) && !blockstate.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockstate) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
        mc.level.addParticle(iparticledata, (double) motionBlockingHeightMinus1.getX() + randDouble, (double) motionBlockingHeightMinus1.getY() + particleMaxAddedY, (double) motionBlockingHeightMinus1.getZ() + randDouble2, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean drippingLeaves() {
        return true;
    }

    @Override
    public void clientTick(ClientLevel world, int tickSpeed, long worldTime, Minecraft mc, Predicate<ResourceLocation> biomePredicate) {

    }
}
