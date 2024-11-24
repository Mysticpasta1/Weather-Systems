package corgitaco.betterweather.weather.event.client;

import corgitaco.betterweather.weather.event.client.settings.AcidRainClientSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

public class AcidRainClient extends RainClient {

    private final boolean addSmokeParticles;

    public AcidRainClient(AcidRainClientSettings clientSettings) {
        super(clientSettings);
        this.addSmokeParticles = clientSettings.addSmokeParticles;
    }

    @Override
    protected void addParticlesToWorld(Minecraft mc, BlockPos motionBlockingHeightMinus1, double randDouble, double randDouble2, BlockState blockstate, FluidState fluidstate, double particleMaxAddedY) {
        if (addSmokeParticles) {
            mc.level.addParticle(ParticleTypes.SMOKE, (double) motionBlockingHeightMinus1.getX() + randDouble, (double) motionBlockingHeightMinus1.getY() + particleMaxAddedY, (double) motionBlockingHeightMinus1.getZ() + randDouble2, 0.0D, 0.0D, 0.0D);
        }
        addAcidRainParticles(mc.gameRenderer.getMainCamera(), mc, mc.levelRenderer);
        super.addParticlesToWorld(mc, motionBlockingHeightMinus1, randDouble, randDouble2, blockstate, fluidstate, particleMaxAddedY);
    }

    public static void addAcidRainParticles(Camera activeRenderInfoIn, Minecraft mc, LevelRenderer worldRenderer) {
        float f = mc.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
        if (!(f <= 0.0F)) {
            Random random = new Random((long) (mc.getPartialTick() * 312987231L));
            LevelReader iworldreader = mc.level;
            BlockPos blockpos = activeRenderInfoIn.getBlockPosition();
            BlockPos blockpos1 = null;
            int i = (int) (100.0F * f * f) / (mc.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

            for (int j = 0; j < i; ++j) {
                int k = random.nextInt(21) - 10;
                int l = random.nextInt(21) - 10;
                BlockPos blockpos2 = iworldreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l)).below();
                Holder<Biome> biome = iworldreader.getBiome(blockpos2);
                if (blockpos2.getY() > 0 && blockpos2.getY() <= blockpos.getY() + 10 && blockpos2.getY() >= blockpos.getY() - 10 && biome.value().getPrecipitationAt(blockpos) == Biome.Precipitation.RAIN && biome.value().getBaseTemperature() >= 0.15F) {
                    blockpos1 = blockpos2;
                    if (mc.options.particles().get() == ParticleStatus.MINIMAL) {
                        break;
                    }

                    double d0 = random.nextDouble();
                    double d1 = random.nextDouble();
                    BlockState blockstate = iworldreader.getBlockState(blockpos2);
                    FluidState fluidstate = iworldreader.getFluidState(blockpos2);
                    VoxelShape voxelshape = blockstate.getCollisionShape(iworldreader, blockpos2);
                    double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                    double d3 = (double) fluidstate.getHeight(iworldreader, blockpos2);
                    double d4 = Math.max(d2, d3);
                    ParticleOptions iparticledata = !fluidstate.is(FluidTags.LAVA) && !blockstate.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.canLight(blockstate) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
                    mc.level.addParticle(iparticledata, (double) blockpos2.getX() + d0, (double) blockpos2.getY() + d4, (double) blockpos2.getZ() + d1, 0.0D, 0.0D, 0.0D);
                }
            }

            if (blockpos1 != null && random.nextInt(3) < worldRenderer.rainSoundTime++) {
                worldRenderer.rainSoundTime = 0;
                if (blockpos1.getY() > blockpos.getY() + 1 && iworldreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float) blockpos.getY())) {
                    mc.level.playSound(mc.player, blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F);
                } else {
                    mc.level.playSound(mc.player, blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F);
                }
            }
        }
    }
}
