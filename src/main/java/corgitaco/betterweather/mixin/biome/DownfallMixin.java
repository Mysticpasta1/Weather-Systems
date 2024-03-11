package corgitaco.betterweather.mixin.biome;

import corgitaco.betterweather.helpers.BiomeHelper;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.ClimateSettings.class)
public class DownfallMixin {
    @Shadow @Final private float downfall;

    @Inject(method = "downfall", at = @At("RETURN"), cancellable = true)
    private void modifyDownfall(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(this.downfall + (float) new BiomeHelper().getHumidityModifier());
    }
}
