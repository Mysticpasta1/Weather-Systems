package corgitaco.betterweather;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.api.weather.WeatherClientSettings;
import corgitaco.betterweather.weather.event.client.settings.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public record WeatherClientSettingType<T extends WeatherClientSettings>(Codec<T> codec) {
    public static final ResourceKey<Registry<WeatherClientSettingType<?>>> KEY = ResourceKey.<WeatherClientSettingType<?>>createRegistryKey(new ResourceLocation(BetterWeather.MOD_ID, "weather"));
    public static final DeferredRegister<WeatherClientSettingType<?>> DEFERRED_REGISTER = DeferredRegister.create(KEY, BetterWeather.MOD_ID);
    public static final Supplier<IForgeRegistry<WeatherClientSettingType<?>>> REGISTRY = DEFERRED_REGISTER.makeRegistry(() -> new RegistryBuilder<WeatherClientSettingType<?>>().allowModification());
    public static final Codec<WeatherClientSettings> CODEC = REGISTRY.get().getCodec().dispatch(WeatherClientSettings::type, WeatherClientSettingType::codec);

    public static final RegistryObject<WeatherClientSettingType<AcidRainClientSettings>> ACID_RAIN_CLIENT = register("acid_rain", AcidRainClientSettings.CODEC);
    public static final RegistryObject<WeatherClientSettingType<BlizzardClientSettings>> BLIZZARD_CLIENT = register("blizzard", BlizzardClientSettings.CODEC);
    public static final RegistryObject<WeatherClientSettingType<CloudyClientSettings>> CLOUDY_CLIENT = register("cloudy", CloudyClientSettings.CODEC);
    public static final RegistryObject<WeatherClientSettingType<SunnyClientSettings>> NONE_CLIENT = register("none", SunnyClientSettings.CODEC);
    public static final RegistryObject<WeatherClientSettingType<RainClientSettings>> RAIN_CLIENT = register("rain", RainClientSettings.CODEC);

    public static <T extends WeatherClientSettings> RegistryObject<WeatherClientSettingType<T>> register(String name, Codec<T> codec) {
        return DEFERRED_REGISTER.register(name, () -> new WeatherClientSettingType<>(codec));
    }

    public static void register(IEventBus bus) {
        DEFERRED_REGISTER.register(bus);
    }
}
