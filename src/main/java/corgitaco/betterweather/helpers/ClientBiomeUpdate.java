package corgitaco.betterweather.helpers;

import corgitaco.betterweather.api.weather.WeatherEventContext;
import corgitaco.betterweather.api.weather.WeatherEventSettings;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class ClientBiomeUpdate {
    RegistryAccess registryAccess;
    BWWeatherEventContext weatherContext;

    public ClientBiomeUpdate(RegistryAccess registryAccess, BWWeatherEventContext bwWeatherEventContext) {
        this.registryAccess = registryAccess;
        this.weatherContext = bwWeatherEventContext;
    }

    public void updateBiomeData() {
        var weather = this.weatherContext.getCurrentEvent();

        for (Map.Entry<ResourceKey<Biome>, Biome> entry : this.registryAccess.registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey()).entrySet()) {
            Biome biome = entry.getValue();
            float weatherHumidityModifier = weather == null ? 0.0F : (float) weather.getHumidityModifierAtPosition(null);
            float weatherTemperatureModifier = weather == null ? 0.0F : (float) weather.getTemperatureModifierAtPosition(null);

            ((BiomeModifier) biome).setWeatherTempModifier(weatherTemperatureModifier);
            ((BiomeModifier) biome).setWeatherHumidityModifier(weatherHumidityModifier);
        }
    }

    private WeatherEventSettings getCurrentWeatherEventSettings(BWWeatherEventContext weatherContext) {
        return null;
    }
}
