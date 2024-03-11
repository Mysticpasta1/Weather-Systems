package corgitaco.betterweather.mixin.server.commands;

import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.WeatherCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherCommand.class)
public abstract class MixinWeatherCommand {

    @Inject(method = "setRain", at = @At("HEAD"), cancellable = true)
    private static void cancelRain(CommandSourceStack source, int p_139179_, CallbackInfoReturnable<Integer> cir) {
        if (((BetterWeatherWorldData) source.getLevel()).getWeatherEventContext() != null) {
            source.sendSuccess(() -> Component.translatable("commands.bw.vanillaweather.fail"), true);
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "setClear", at = @At("HEAD"), cancellable = true)
    private static void cancelClear(CommandSourceStack source, int p_139174_, CallbackInfoReturnable<Integer> cir) {
        if (((BetterWeatherWorldData) source.getLevel()).getWeatherEventContext() != null) {
            source.sendSuccess(() -> Component.translatable("commands.bw.vanillaweather.fail"), true);
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "setThunder", at = @At("HEAD"), cancellable = true)
    private static void cancelThunder(CommandSourceStack source, int p_139184_, CallbackInfoReturnable<Integer> cir) {
        if (((BetterWeatherWorldData) source.getLevel()).getWeatherEventContext() != null) {
            source.sendSuccess(() -> Component.translatable("commands.bw.vanillaweather.fail"), true);
            cir.setReturnValue(0);
        }
    }
}
