package corgitaco.betterweather.mixin.biome;

import corgitaco.betterweather.api.BiomeClimate;
import corgitaco.betterweather.helpers.BiomeHelper;
import corgitaco.betterweather.helpers.BiomeModifier;
import corgitaco.betterweather.helpers.BiomeHelperSetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Biome.class)
public abstract class MixinBiome implements BiomeModifier, BiomeClimate {
    @Unique private BiomeHelper helper = new BiomeHelper();

    @Shadow
    @Final
    private Biome.ClimateSettings climateSettings;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(Biome.ClimateSettings p_220530_, BiomeSpecialEffects p_220531_, BiomeGenerationSettings p_220532_, MobSpawnSettings p_220533_, CallbackInfo ci) {
        ((BiomeHelperSetter) (Object) p_220530_).setBiomeHelper(helper);
    }

    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void modifyTemperature(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(this.climateSettings.temperature() + (float) helper.getTemperatureModifier());
    }

    @Override
    public double getTemperatureModifier() {
        return helper.getTemperatureModifier();
    }

    @Override
    public double getWeatherTemperatureModifier(BlockPos pos) {
        return helper.getWeatherTemperatureModifier(pos);
    }

    @Override
    public double getHumidityModifier() {
        return helper.getHumidityModifier();
    }

    @Override
    public double getWeatherHumidityModifier(BlockPos pos) {
        return helper.getWeatherHumidityModifier(pos);
    }

    @Override
    public void setWeatherTempModifier(float tempModifier) {
        helper.setWeatherTempModifier(tempModifier);
    }

    @Override
    public void setWeatherHumidityModifier(float humidityModifier) {
        helper.setWeatherHumidityModifier(humidityModifier);
    }
}
