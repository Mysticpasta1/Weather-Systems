package corgitaco.betterweather.data.network.packet.weather;

import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.helpers.ClientBiomeUpdate;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

@SuppressWarnings("ClassCanBeRecord")
public class WeatherContextConstructingPacket {

    private final BWWeatherEventContext bwWeatherEventContext;

    public WeatherContextConstructingPacket(BWWeatherEventContext bwWeatherEventContext) {
        this.bwWeatherEventContext = bwWeatherEventContext;
    }

    public static void encode(WeatherContextConstructingPacket packet, FriendlyByteBuf buf) {
        buf.writeWithCodec(NbtOps.INSTANCE, BWWeatherEventContext.PACKET_CODEC, packet.bwWeatherEventContext);
    }

    public static WeatherContextConstructingPacket decode(FriendlyByteBuf buf) {
        return new WeatherContextConstructingPacket(buf.readWithCodec(NbtOps.INSTANCE, BWWeatherEventContext.PACKET_CODEC));
    }

    public static void handle(WeatherContextConstructingPacket message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();

                ClientLevel world = minecraft.level;
                if (world != null && minecraft.player != null) {
                    BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) world).getWeatherEventContext();
                    if (weatherEventContext == null) {
                        weatherEventContext = ((BetterWeatherWorldData) world).setWeatherEventContext(new BWWeatherEventContext(message.bwWeatherEventContext.getCurrentWeatherEventKey(),
                                message.bwWeatherEventContext.isWeatherForced(), world.dimension().location(), world.registryAccess().registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey()), message.bwWeatherEventContext.getWeatherEvents()));
                        weatherEventContext.setCurrentEvent(message.bwWeatherEventContext.getCurrentEvent());
                        new ClientBiomeUpdate(world.registryAccess(), message.bwWeatherEventContext).updateBiomeData();
                    } else {
                        throw new UnsupportedOperationException("There is already a weather event context constructed for this world!");
                    }
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}