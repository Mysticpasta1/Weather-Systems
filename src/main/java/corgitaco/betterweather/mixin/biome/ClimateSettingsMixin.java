package corgitaco.betterweather.mixin.biome;

import corgitaco.betterweather.helpers.BiomeHelper;
import corgitaco.betterweather.helpers.BiomeHelperSetter;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.ClimateSettings.class)
public class ClimateSettingsMixin implements BiomeHelperSetter {
    @Shadow @Final private float downfall;

    @Unique private BiomeHelper helper;

    @Inject(method = "downfall", at = @At("RETURN"), cancellable = true)
    private void modifyDownfall(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(this.downfall + (float) helper.getHumidityModifier());
    }

    @Override
    public void setBiomeHelper(BiomeHelper helper) {
        this.helper = helper;
    }
}
