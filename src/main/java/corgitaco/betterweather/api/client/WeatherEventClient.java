package corgitaco.betterweather.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.util.Textures;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;

import static corgitaco.betterweather.api.client.WeatherEventClient.LegacyWeatherRendering.*;

public abstract class WeatherEventClient<T extends WeatherClientSettings> {

    private final ColorSettings colorSettings;
    private final float skyOpacity;
    private final float fogDensity;
    private final boolean sunsetSunriseColor;

    protected final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    public WeatherEventClient(T clientSettings) {
        this.colorSettings = clientSettings.getColorSettings();
        this.skyOpacity = clientSettings.skyOpacity();
        this.fogDensity = clientSettings.fogDensity();
        this.sunsetSunriseColor = clientSettings.sunsetSunriseColor();
    }

    public void renderWeather(Minecraft mc, ClientLevel world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z, Predicate<ResourceKey<Biome>> biomePredicate) {
        (switch (BWWeatherEventContext.currentEvent.getName()) {
            case "sunny", "partly_cloudy", "cloudy", "overcast", "heat_lightning" -> CLEAR;
            case "drizzle", "rain", "downpour", "lightning" -> RAIN;
            case "acid_drizzle", "acid_rain", "acid_downpour", "acid_lightning" -> ACIDIC;
            case "dusting" ,"snow", "blizzard", "thunder_blizzard" -> SNOWY;
            default -> throw new IllegalStateException("Unexpected value: " + BWWeatherEventContext.currentEvent.getName());
        }).render(mc, world, lightTexture, ticks, partialTicks, x, y, z, biomePredicate);
    }

    public enum LegacyWeatherRendering {
        CLEAR,
        RAIN {
            @Override
            public void render(Minecraft mc, ClientLevel world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z, Predicate<ResourceKey<Biome>> biomePredicate) {
                renderVanillaWeather(mc, partialTicks, lightTexture, x, y, z, Textures.RAIN_LOCATION, Textures.SNOW_LOCATION, ticks, biomePredicate);
            }
        },
        ACIDIC {
            @Override
            public void render(Minecraft mc, ClientLevel world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z, Predicate<ResourceKey<Biome>> biomePredicate) {
                renderVanillaWeather(mc, partialTicks, lightTexture, x, y, z, Textures.ACID_RAIN_LOCATION, Textures.SNOW_LOCATION, ticks, biomePredicate);
            }
        },
        SNOWY {
            @Override
            public void render(Minecraft mc, ClientLevel world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z, Predicate<ResourceKey<Biome>> biomePredicate) {
                renderVanillaWeather(mc, partialTicks, lightTexture, x, y, z, Textures.SNOW_LOCATION, Textures.SNOW_LOCATION, ticks, biomePredicate);
            }
        };

        public void render(Minecraft mc, ClientLevel world, LightTexture lightTexture, int ticks, float partialTicks, double x, double y, double z, Predicate<ResourceKey<Biome>> biomePredicate) {
        }
    }

    public abstract void clientTick(ClientLevel world, int tickSpeed, long worldTime, Minecraft mc, Predicate<ResourceLocation> biomePredicate);

    public boolean sunsetSunriseColor() {
        return sunsetSunriseColor;
    }

    public float skyOpacity() {
        return Mth.clamp(skyOpacity, 0.0F, 1.0F);
    }

    public float dayLightDarkness() {
        return fogDensity;
    }

    public boolean drippingLeaves() {
        return false;
    }

    public float fogDensity() {
        return fogDensity;
    }

    public ColorSettings getColorSettings() {
        return colorSettings;
    }

    @OnlyIn(Dist.CLIENT)
    public float skyOpacity(ClientLevel world, BlockPos playerPos, Predicate<ResourceKey<Biome>> isValidBiome) {
        return mixer(world, playerPos, 12, 2.0F, 1.0F - skyOpacity, isValidBiome);
    }

    @OnlyIn(Dist.CLIENT)
    public float fogDensity(ClientLevel world, BlockPos playerPos, Predicate<ResourceKey<Biome>> isValidBiome) {
        return mixer(world, playerPos, 12, 0.1F, fogDensity, isValidBiome);
    }


    private float mixer(ClientLevel world, BlockPos playerPos, int transitionRange, float weight, float targetMaxValue, Predicate<ResourceKey<Biome>> validBiomes) {
        int x = playerPos.getX();
        int z = playerPos.getZ();
        float accumulated = 0.0F;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int sampleX = x - transitionRange; sampleX <= x + transitionRange; ++sampleX) {
            pos.setX(sampleX);

            for (int sampleZ = z - transitionRange; sampleZ <= z + transitionRange; ++sampleZ) {
                pos.setZ(sampleZ);

                Holder<Biome> biome = world.getBiome(pos);
                if (validBiomes.test(biome.unwrapKey().get())) {

                    accumulated += weight * weight;
                }
            }
        }
        float transitionSmoothness = 33 * 33;
        return Math.min(targetMaxValue, (float) Math.sqrt(accumulated / transitionSmoothness));
    }

    @OnlyIn(Dist.CLIENT)
    public float cloudBlendStrength(ClientLevel world, BlockPos playerPos, Predicate<ResourceKey<Biome>> isValidBiome) {
        return mixer(world, playerPos, 15, 1.2F, (float) this.getColorSettings().getCloudColorBlendStrength(), isValidBiome);
    }

    public static void renderVanillaWeather(Minecraft mc, float p_109705_, LightTexture p_109704_, double p_109706_, double p_109707_, double p_109708_, ResourceLocation rainTexture, ResourceLocation snowTexture, int ticks, Predicate<ResourceKey<Biome>> isValidBiome) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        float[] rainSizeX = new float[1024];
        float[] rainSizeZ = new float[1024];

        for(int i = 0; i < 32; ++i) {
            for(int j = 0; j < 32; ++j) {
                float f = (float)(j - 16);
                float f1 = (float)(i - 16);
                float f2 = Mth.sqrt(f * f + f1 * f1);
                rainSizeX[i << 5 | j] = -f1 / f2;
                rainSizeZ[i << 5 | j] = f / f2;
            }
        }

        float f = level.getRainLevel(p_109705_);
        if (!(f <= 0.0F)) {
            p_109704_.turnOnLightLayer();
            int i = Mth.floor(p_109706_);
            int j = Mth.floor(p_109707_);
            int k = Mth.floor(p_109708_);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            int l = 5;
            if (Minecraft.useFancyGraphics()) {
                l = 10;
            }

            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            int i1 = -1;
            float f1 = ticks + p_109705_;
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for(int j1 = k - l; j1 <= k + l; ++j1) {
                for(int k1 = i - l; k1 <= i + l; ++k1) {
                    int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
                    double d0 = (double)rainSizeX[l1] * 0.5D;
                    double d1 = (double)rainSizeZ[l1] * 0.5D;
                    blockpos$mutableblockpos.set((double)k1, p_109707_, (double)j1);
                    Biome biome = level.getBiome(blockpos$mutableblockpos).value();
                    if (biome.getPrecipitationAt(blockpos$mutableblockpos) != Biome.Precipitation.NONE) {
                        int i2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, k1, j1);
                        int j2 = j - l;
                        int k2 = j + l;
                        if (j2 < i2) {
                            j2 = i2;
                        }

                        if (k2 < i2) {
                            k2 = i2;
                        }

                        int l2 = i2;
                        if (i2 < j) {
                            l2 = j;
                        }

                        if (j2 != k2) {
                            RandomSource randomsource = RandomSource.create((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
                            blockpos$mutableblockpos.set(k1, j2, j1);
                            if (biome.warmEnoughToRain(blockpos$mutableblockpos)) {
                                if (i1 != 0) {
                                    if (i1 >= 0) {
                                        tesselator.end();
                                    }

                                    i1 = 0;
                                    RenderSystem.setShaderTexture(0, rainTexture);
                                    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                }

                                int i3 = ticks + k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 31;
                                float f2 = -((float)i3 + p_109705_) / 32.0F * (3.0F + randomsource.nextFloat());
                                double d2 = (double)k1 + 0.5D - p_109706_;
                                double d4 = (double)j1 + 0.5D - p_109708_;
                                float f3 = (float)Math.sqrt(d2 * d2 + d4 * d4) / (float)l;
                                float f4 = ((1.0F - f3 * f3) * 0.5F + 0.5F) * f;
                                blockpos$mutableblockpos.set(k1, l2, j1);
                                int j3 = LevelRenderer.getLightColor(level, blockpos$mutableblockpos);
                                bufferbuilder.vertex((double)k1 - p_109706_ - d0 + 0.5D, (double)k2 - p_109707_, (double)j1 - p_109708_ - d1 + 0.5D).uv(0.0F, (float)j2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ + d0 + 0.5D, (double)k2 - p_109707_, (double)j1 - p_109708_ + d1 + 0.5D).uv(1.0F, (float)j2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ + d0 + 0.5D, (double)j2 - p_109707_, (double)j1 - p_109708_ + d1 + 0.5D).uv(1.0F, (float)k2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ - d0 + 0.5D, (double)j2 - p_109707_, (double)j1 - p_109708_ - d1 + 0.5D).uv(0.0F, (float)k2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                            } else {
                                if (i1 != 1) {
                                    if (i1 >= 0) {
                                        tesselator.end();
                                    }

                                    i1 = 1;
                                    RenderSystem.setShaderTexture(0, snowTexture);
                                    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                }

                                float f5 = -((float)(ticks & 511) + p_109705_) / 512.0F;
                                float f6 = (float)(randomsource.nextDouble() + (double)f1 * 0.01D * (double)((float)randomsource.nextGaussian()));
                                float f7 = (float)(randomsource.nextDouble() + (double)(f1 * (float)randomsource.nextGaussian()) * 0.001D);
                                double d3 = (double)k1 + 0.5D - p_109706_;
                                double d5 = (double)j1 + 0.5D - p_109708_;
                                float f8 = (float)Math.sqrt(d3 * d3 + d5 * d5) / (float)l;
                                float f9 = ((1.0F - f8 * f8) * 0.3F + 0.5F) * f;
                                blockpos$mutableblockpos.set(k1, l2, j1);
                                int k3 = LevelRenderer.getLightColor(level, blockpos$mutableblockpos);
                                int l3 = k3 >> 16 & '\uffff';
                                int i4 = k3 & '\uffff';
                                int j4 = (l3 * 3 + 240) / 4;
                                int k4 = (i4 * 3 + 240) / 4;
                                bufferbuilder.vertex((double)k1 - p_109706_ - d0 + 0.5D, (double)k2 - p_109707_, (double)j1 - p_109708_ - d1 + 0.5D).uv(0.0F + f6, (float)j2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ + d0 + 0.5D, (double)k2 - p_109707_, (double)j1 - p_109708_ + d1 + 0.5D).uv(1.0F + f6, (float)j2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ + d0 + 0.5D, (double)j2 - p_109707_, (double)j1 - p_109708_ + d1 + 0.5D).uv(1.0F + f6, (float)k2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                                bufferbuilder.vertex((double)k1 - p_109706_ - d0 + 0.5D, (double)j2 - p_109707_, (double)j1 - p_109708_ - d1 + 0.5D).uv(0.0F + f6, (float)k2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                            }
                        }
                    }
                }
            }

            if (i1 >= 0) {
                tesselator.end();
            }

            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            p_109704_.turnOffLightLayer();
        }
    }
}
