package corgitaco.betterweather.weather.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.WeatherType;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class Sunny extends Weather {

    public static final Codec<Sunny> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            WeatherClientSettings.CODEC.fieldOf("clientSettings").forGetter(Weather::getClientSettings))
            .apply(builder, Sunny::new));

    public Sunny(WeatherClientSettings clientSettings) {
        super(clientSettings, new BasicSettings("ALL", 0.0, 0.0, 0.0, false, 0), DecaySettings.NONE);
    }

    @Override
    public void worldTick(ServerLevel world, int tickSpeed, long worldTime) {

    }

    @Override
    public WeatherType<?> type() {
        return WeatherType.NONE.get();
    }

    @Override
    public double getTemperatureModifierAtPosition(BlockPos pos) {
        return getTemperatureOffsetRaw();
    }

    @Override
    public double getHumidityModifierAtPosition(BlockPos pos) {
        return getHumidityOffsetRaw();
    }
}
