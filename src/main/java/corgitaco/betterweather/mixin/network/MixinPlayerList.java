package corgitaco.betterweather.mixin.network;

import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.data.network.NetworkHandler;
import corgitaco.betterweather.data.network.packet.weather.WeatherContextConstructingPacket;
import corgitaco.betterweather.data.storage.WeatherEventSavedData;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {
    @Inject(method = "sendLevelInfo", at = @At(value = "HEAD"))
    private void sendContext(ServerPlayer playerIn, ServerLevel worldIn, CallbackInfo ci) {
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) worldIn).getWeatherEventContext();
        if (weatherEventContext == null) {
            weatherEventContext = ((BetterWeatherWorldData) worldIn).setWeatherEventContext(new BWWeatherEventContext(WeatherEventSavedData.get(worldIn).getEvent(), WeatherEventSavedData.get(worldIn).isWeatherForced(), worldIn.dimension().location(), worldIn.registryAccess().registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey()), BetterWeatherRegistry.getWeather()));
        }
        if (weatherEventContext != null) {
            NetworkHandler.sendToPlayer(playerIn, new WeatherContextConstructingPacket(weatherEventContext));
        }
    }
}
