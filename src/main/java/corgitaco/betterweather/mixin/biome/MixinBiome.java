package corgitaco.betterweather.mixin.biome;

import corgitaco.betterweather.api.BiomeClimate;
import corgitaco.betterweather.helpers.BiomeHelper;
import corgitaco.betterweather.helpers.BiomeModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Biome.class)
public abstract class MixinBiome implements BiomeModifier, BiomeClimate {

    @Shadow
    @Final
    private Biome.ClimateSettings climateSettings;

    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void modifyTemperature(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(this.climateSettings.temperature() + (float) new BiomeHelper().getTemperatureModifier());
    }

    @Override
    public double getTemperatureModifier() {
        return new BiomeHelper().getTemperatureModifier();
    }

    @Override
    public double getWeatherTemperatureModifier(BlockPos pos) {
        return new BiomeHelper().getWeatherTemperatureModifier(pos);
    }

    @Override
    public double getHumidityModifier() {
        return new BiomeHelper().getHumidityModifier();
    }

    @Override
    public double getWeatherHumidityModifier(BlockPos pos) {
        return new BiomeHelper().getWeatherHumidityModifier(pos);
    }

    @Override
    public void setWeatherTempModifier(float tempModifier) {
        new BiomeHelper().setWeatherTempModifier(tempModifier);
    }

    @Override
    public void setWeatherHumidityModifier(float humidityModifier) {
        new BiomeHelper().setWeatherHumidityModifier(humidityModifier);
    }
}
