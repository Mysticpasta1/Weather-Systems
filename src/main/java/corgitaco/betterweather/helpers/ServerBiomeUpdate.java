package corgitaco.betterweather.helpers;

import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

/**
 * May only be called from the Server and is only castable to or extenders of {@link net.minecraft.server.level.ServerLevel}.
 */
public class ServerBiomeUpdate {
    ServerChunkCache chunkSource;
    RegistryAccess registryAccess;
    BWWeatherEventContext weatherContext;
    public ServerBiomeUpdate(ServerChunkCache chunkSource, RegistryAccess registryAccess, BWWeatherEventContext weatherContext) {
        this.chunkSource = chunkSource;
        this.registryAccess = registryAccess;
        this.weatherContext = weatherContext;
    }

    public void updateBiomeData() {
        Set<Holder<Biome>> validBiomes = this.chunkSource.getGenerator().getBiomeSource().possibleBiomes();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : this.registryAccess.registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey()).entrySet()) {
            Biome biome = entry.getValue();
            ResourceKey<Biome> biomeKey = entry.getKey();

            Weather weather = weatherContext != null ? weatherContext.getCurrentEvent() : null;

            if (weather != null && validBiomes.stream().anyMatch(a -> a.is(biomeKey)) && weather.isValidBiome(biomeKey)) {
                float weatherHumidityModifier = (float) weather.getHumidityModifierAtPosition(null);
                float weatherTemperatureModifier = (float) weather.getTemperatureModifierAtPosition(null);
                ((BiomeModifier) (Object) biome).setWeatherTempModifier(weatherTemperatureModifier);
                ((BiomeModifier) (Object) biome).setWeatherHumidityModifier(weatherHumidityModifier);
            }
        }
    }
}
