package corgitaco.betterweather.client.audio;

import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherEventAudio;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class WeatherSoundHandler implements AmbientSoundHandler {

    private final Player player;
    private final SoundManager soundHandler;
    private final BiomeManager biomeManager;
    private final ClientLevel world;
    private WeatherSoundHandler.Sound currentSound;

    public WeatherSoundHandler(AbstractClientPlayer player, SoundManager soundHandler, BiomeManager biomeManager) {
        this.player = player;
        this.soundHandler = soundHandler;
        this.biomeManager = biomeManager;
        this.world = (ClientLevel) player.level();
    }

    @Override
    public void tick() {
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) player.level()).getWeatherEventContext();
        if (weatherEventContext == null) {
            return;
        }

        Weather currentEvent = weatherEventContext.getCurrentEvent();
        WeatherClientSettings clientSettings = currentEvent.getClientSettings();
        if (clientSettings instanceof WeatherEventAudio) {
            ResourceLocation currentBiome = world.getBiome(player.blockPosition()).unwrapKey().get().location();
            if (currentEvent.isValidBiome(currentBiome)) {
                if (this.currentSound == null) {
                    this.currentSound = new WeatherSoundHandler.Sound(((WeatherEventAudio) clientSettings).getSound(), this.world, ((WeatherEventAudio) clientSettings).getVolume(), ((WeatherEventAudio) clientSettings).getPitch());
                    this.soundHandler.play(this.currentSound);
                }

                if (world.rainLevel == 1.0) {
                    this.currentSound.fadeInSound();
                }

                if(currentEvent.getName().equals("cloudy") || currentEvent.getName().equals("cloudy_thundering")) {
                    endAudio();
                }

            } else {
                endAudio();
            }
        } else {
            endAudio();
        }
    }

    private void endAudio() {
        if (this.currentSound != null) {
            this.currentSound.fadeOutSound();
            if (this.currentSound.isStopped()) {
                this.currentSound = null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Sound extends AbstractTickableSoundInstance {
        private static final Vector3d[] vector3ds = vectorDistribution(1000);

        private final ClientLevel world;
        private final float maxVolume;

        private int fadeSpeed;
        private int fadeInTicks;

        private boolean overrideRainStrength = false;
        private float decreasedVolume;

        private int fadeLimit = 40;


        public Sound(SoundEvent sound, ClientLevel world, float volume, float pitch) {
            super(sound, SoundSource.WEATHER, RandomSource.create());
            this.world = world;
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.maxVolume = volume;

        }

        /**
         * Python script for visualising where the sound is being sampled from
         * <p>
         * import math
         * <p>
         * from matplotlib import pyplot
         * from mpl_toolkits.mplot3d import Axes3D
         * <p>
         * import matplotlib.pyplot as plt
         * from mpl_toolkits.mplot3d import Axes3D
         * fig = plt.figure()
         * ax = fig.add_subplot(111, projection='3d')
         * <p>
         * <p>
         * def fibonacci_sphere(samples=1):
         * <p>
         * points = []
         * phi = math.pi * (3. - math.sqrt(5.))  # golden angle in radians
         * <p>
         * for i in range(int(samples/4), samples):
         * y = 1 - (i / float(samples - 1)) * 2  # y goes from 1 to -1
         * radius = math.sqrt(1 - y * y)  # radius at y
         * <p>
         * theta = phi * i  # golden angle increment
         * <p>
         * x = math.cos(theta) * radius
         * z = math.sin(theta) * radius
         * <p>
         * points.append((x, y, z))
         * <p>
         * return points
         * <p>
         * xvals = []
         * yvals = []
         * zvals = []
         * for point in fibonacci_sphere(10000):
         * xvals.append(point[0])
         * yvals.append(point[1])
         * zvals.append(point[2])
         * <p>
         * fig = pyplot.figure()
         * ax = Axes3D(fig)
         * <p>
         * ax.scatter(xvals, yvals, zvals)
         * pyplot.show()
         */
        private static Vector3d[] vectorDistribution(int samples) {
            List<Vector3d> points = new ArrayList<>();
            double phi = Math.PI * (3. - Math.sqrt(5.));  // golden angle in radians

            for (int i = samples / 4; i < samples; i++) {
                double y = 1 - (i / (float) (samples - 1)) * 2; //y goes from 1 to -1
                double radius = Math.sqrt(1 - y * y); //radius at y

                double theta = phi * i; //golden angle increment

                double x = Math.cos(theta) * radius;
                double z = Math.sin(theta) * radius;

                points.add(new Vector3d(x, -y, z));
            }
            return points.toArray(new Vector3d[0]);
        }

        public void tick() {
            if (volume == 0) {
                this.stop();
            }

            Vec3 startPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Player player = Minecraft.getInstance().player;
            byte brightness = (byte) (this.world.getBrightness(LightLayer.SKY, new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z)));
            int vectorDistance = 35;
            double maxDistanceNormalised = 0; //average sample distance

            //Cast ray in every direction sampled
            for (int i = 0, vector3dsLength = vector3ds.length; i < vector3dsLength; i++) {
                vector3ds[i].mul(vectorDistance);
                Vec3 endPos = startPos.add(vector3ds[i].x, vector3ds[i].y, vector3ds[i].z);
                BlockHitResult result = world.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

                if (result.getType() == HitResult.Type.MISS) {
                    maxDistanceNormalised += maxVolume; //if miss, assume max volume
                }

                Vec3 hitVec = result.getLocation();
                Vec3 dif = startPos.subtract(hitVec);
                float distance = (float) dif.length() / vectorDistance; //normalised distance
                //distance is weighted such that longer distance count more, based on skylight brightness
                maxDistanceNormalised += Mth.lerp(brightness / 15f, startPos.y < world.getSeaLevel() ? 0.0000000000001 : distance, Math.pow(distance, 1 / 4f /*Controls the weighting based on distance*/));
            }
            this.decreasedVolume = (float) (maxDistanceNormalised / vector3ds.length) * (player.isEyeInFluid(FluidTags.WATER) || player.isEyeInFluid(FluidTags.LAVA) ? 0.2F : 1);


            this.fadeInTicks += this.fadeSpeed;
            this.volume = Mth.clamp(world.getRainLevel(Minecraft.getInstance().getFrameTime()), 0.0F, decreasedVolume);
        }

        public void fadeOutSound() {
            this.fadeInTicks = Math.min(this.fadeInTicks, fadeLimit);
            this.fadeSpeed = -1;
        }

        public void fadeInSound() {
            this.fadeInTicks = Math.max(0, this.fadeInTicks);
            this.fadeSpeed = 1;
        }
    }
}
