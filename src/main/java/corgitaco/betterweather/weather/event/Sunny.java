package corgitaco.betterweather.weather.event;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.BiomeCheck;
import corgitaco.betterweather.weather.event.client.settings.SunnyClientSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;

import java.util.Collections;

public class Sunny extends Weather {
    public static final Sunny INSTANCE = new Sunny(new SunnyClientSettings(new ColorSettings("#00ff00", 0.5, "#00ff00", 0.5)));

    public static final Codec<Sunny> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            WeatherClientSettings.CODEC.fieldOf("clientSettings").forGetter(Weather::getClientSettings))
            .apply(builder, Sunny::new));
    public static final ResourceLocation KEY = new ResourceLocation(BetterWeather.MOD_ID, "sunny");

    public Sunny(JsonObject json) {
        this(
                WeatherClientSettings.fromJson(GsonHelper.getAsJsonObject(json, "clientSettings"))
        );
    }

    public Sunny(WeatherClientSettings clientSettings) {
        super(clientSettings, new BasicSettings(Collections.singletonList(BiomeCheck.AlwaysTrueCheck.INSTANCE), 0.0, 0.0, 0.0, false, 0, Collections.emptyMap(), false), DecaySettings.NONE);
    }

    @Override
    public void worldTick(ServerLevel world, int tickSpeed, long worldTime) {

    }

    @Override
    public WeatherType<?> type() {
        return WeatherType.SUNNY;
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
