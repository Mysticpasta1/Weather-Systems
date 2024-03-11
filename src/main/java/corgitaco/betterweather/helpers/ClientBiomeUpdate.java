package corgitaco.betterweather.helpers;

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
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : this.registryAccess.registryOrThrow(ForgeRegistries.BIOMES.getRegistryKey()).entrySet()) {
            Biome biome = entry.getValue();
            ResourceKey<Biome> biomeKey = entry.getKey();
            float weatherHumidityModifier = weatherContext == null ? 0.0F : (float) this.weatherContext.getCurrentWeatherEventSettings().getHumidityModifierAtPosition(null);
            float weatherTemperatureModifier = weatherContext == null ? 0.0F : (float) this.weatherContext.getCurrentWeatherEventSettings().getTemperatureModifierAtPosition(null);

            ((BiomeModifier) (Object) biome).setWeatherTempModifier(weatherTemperatureModifier);
            ((BiomeModifier) (Object) biome).setWeatherHumidityModifier(weatherHumidityModifier);
        }
    }
}
