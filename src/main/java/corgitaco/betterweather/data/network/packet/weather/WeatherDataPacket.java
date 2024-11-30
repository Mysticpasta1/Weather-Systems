package corgitaco.betterweather.data.network.packet.weather;

import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public class WeatherDataPacket {

    private final ResourceLocation weatherEvent;
    private final boolean weatherForced;

    public WeatherDataPacket(BWWeatherEventContext bwWeatherEventContext) {
        this(bwWeatherEventContext.getCurrentWeatherEventKey(), bwWeatherEventContext.isWeatherForced());
    }

    public WeatherDataPacket(ResourceLocation weatherEvent, boolean weatherForced) {
        this.weatherEvent = weatherEvent;
        this.weatherForced = weatherForced;
    }

    public static void encode(WeatherDataPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.weatherEvent);
        buf.writeBoolean(packet.weatherForced);
    }

    public static WeatherDataPacket decode(FriendlyByteBuf buf) {
        return new WeatherDataPacket(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(WeatherDataPacket message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();

                ClientLevel world = minecraft.level;
                if (world != null && minecraft.player != null) {
                    BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) world).getWeatherEventContext();
                    if (weatherEventContext == null) {
                        throw new UnsupportedOperationException("There is no weather event context constructed for this world!");
                    } else {
                        weatherEventContext.setCurrentEvent(message.weatherEvent);
                    }
                } else {
                    throw new RuntimeException("world " + world + " or " + minecraft.player + " player is null");
                }
            });
        } else {
            ctx.get().enqueueWork(() -> {
                Player player = ctx.get().getSender();
                if(player != null && player.level() instanceof ServerLevel serverLevel) {
                    BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) serverLevel).getWeatherEventContext();
                    if (weatherEventContext == null) {
                        throw new UnsupportedOperationException("There is no weather event context constructed for this world!");
                    } else {
                        weatherEventContext.setCurrentEvent(message.weatherEvent);
                    }
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}