package corgitaco.betterweather;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.settings.*;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

