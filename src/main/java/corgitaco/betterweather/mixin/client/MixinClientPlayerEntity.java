package corgitaco.betterweather.mixin.client;

import com.mojang.authlib.GameProfile;
import corgitaco.betterweather.client.audio.WeatherSoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinClientPlayerEntity {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void weatherAmbience(ClientLevel p_234112_, GameProfile pGameProfile, CallbackInfo ci) {
         new WeatherSoundHandler((AbstractClientPlayer) (Object) this, Minecraft.getInstance().getSoundManager(), p_234112_.getBiomeManager());
    }
}
