package corgitaco.betterweather.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import corgitaco.betterweather.weather.event.client.CloudyClient;
import corgitaco.betterweather.weather.event.client.RainClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    private int ticks;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ClientLevel level;

    @Shadow public abstract void initOutline();

    @Inject(at = @At("HEAD"), method = "renderSnowAndRain", cancellable = true)
    private void renderWeather(LightTexture lightmapIn, float partialTicks, double x, double y, double z, CallbackInfo ci) {
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) this.level).getWeatherEventContext();
        if (weatherEventContext != null) {
            ci.cancel();
            weatherEventContext.getCurrentClientEvent().renderWeather(weatherEventContext, minecraft, this.level, lightmapIn, ticks, partialTicks, x, y, z, weatherEventContext.getCurrentEvent()::isValidBiome);
        }
    }

    @Inject(at = @At("HEAD"), method = "tickRain", cancellable = true)
    private void stopRainParticles(Camera p_109694_, CallbackInfo ci) {
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) this.level).getWeatherEventContext();
        if (minecraft.level != null && weatherEventContext != null) {
            if(weatherEventContext.getCurrentClientEvent() instanceof CloudyClient) {
                ci.cancel();
            } else if (weatherEventContext.getCurrentClientEvent() instanceof RainClient rainClient) {
                ci.cancel();
                rainClient.weatherParticlesAndSound(p_109694_, this.minecraft, this.ticks, weatherEventContext.getCurrentEvent()::isValidBiome);
            }
        }
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    public float sunRemoval(ClientLevel clientWorld, float delta) {
        float rainStrength = this.level.getRainLevel(delta);
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) this.level).getWeatherEventContext();
        return weatherEventContext != null ? rainStrength * weatherEventContext.getCurrentClientEvent().skyOpacity(clientWorld, this.minecraft.player.blockPosition(), weatherEventContext.getCurrentEvent()::isValidBiome) : rainStrength;
    }



    @Inject(method = "buildClouds", at = @At(value = "HEAD"))
    private void modifyCloudColor(BufferBuilder p_234262_, double cloudsX, double cloudsY, double cloudsZ, Vec3 p_234266_, CallbackInfoReturnable<BufferBuilder.RenderedBuffer> cir) {
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) this.level).getWeatherEventContext();
        if (weatherEventContext != null) {
            ColorSettings colorSettings = weatherEventContext.getCurrentEvent().getClientSettings().getColorSettings();
            double cloudColorBlendStrength = colorSettings.getCloudColorBlendStrength();
            if (cloudColorBlendStrength <= 0.0) {
                return;
            }

            int targetCloudHexColor = colorSettings.getTargetCloudHexColor();

            float r = (float) (targetCloudHexColor >> 16 & 255) / 255.0F;
            float g = (float) (targetCloudHexColor >> 8 & 255) / 255.0F;
            float b = (float) (targetCloudHexColor & 255) / 255.0F;

            float blendStrengthAtLocation = weatherEventContext.getCurrentClientEvent().cloudBlendStrength(this.level, new BlockPos((int) cloudsX, (int) cloudsY, (int) cloudsZ), weatherEventContext.getCurrentEvent()::isValidBiome);
            float rainStrength = this.level.getRainLevel(Minecraft.getInstance().getFrameTime());

            float blend = (float) Math.min(cloudColorBlendStrength, rainStrength * blendStrengthAtLocation);
            p_234266_ = new Vec3(Mth.lerp(blend, p_234266_.x, r), Mth.lerp(blend, p_234266_.y, g), Mth.lerp(blend, p_234266_.z, b));
        }
    }
}