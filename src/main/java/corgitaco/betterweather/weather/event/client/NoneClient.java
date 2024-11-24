package corgitaco.betterweather.weather.event.client;

import corgitaco.betterweather.api.client.WeatherEventClient;
import corgitaco.betterweather.weather.event.client.settings.SunnyClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

public class NoneClient extends WeatherEventClient<SunnyClientSettings> {
    public NoneClient(SunnyClientSettings clientSettings) {
        super(clientSettings);
    }

    @Override
    public void clientTick(ClientLevel world, int tickSpeed, long worldTime, Minecraft mc, Predicate<ResourceLocation> biomePredicate) {

    }
}
