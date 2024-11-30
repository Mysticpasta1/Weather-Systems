package corgitaco.betterweather.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @Redirect(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private static float doNotDarkenFogWithRainStrength(ClientLevel world, float delta) {
        return ((BetterWeatherWorldData) world).getWeatherEventContext() != null ? 0.0F : world.getRainLevel(delta);
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void forceWeather(Camera p_234173_, FogRenderer.FogMode p_234174_, float p_234175_, boolean p_234176_, float p_234177_, CallbackInfo ci) {
        ClientLevel world = Minecraft.getInstance().level;
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) world).getWeatherEventContext();
        Weather currentEvent = weatherEventContext != null ? weatherEventContext.getCurrentEvent() : null;


        if (currentEvent != null) {
            float currentFogDensity = currentEvent.getClientSettings().fogDensity();
            float blendedFogDensity = currentEvent.getClient().fogDensity(world, p_234173_.getBlockPosition(), currentEvent::isValidBiome);

            if (currentFogDensity != -1.0F && blendedFogDensity > 0.0F) {
                RenderSystem.setShaderFogStart(Mth.lerp(p_234173_.getEntity().level().getRainLevel(Minecraft.getInstance().getFrameTime()), 0.0F, blendedFogDensity));
                ci.cancel();
            }
        }
    }
}
