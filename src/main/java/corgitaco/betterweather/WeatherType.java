package corgitaco.betterweather;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.weather.event.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public record WeatherType<T extends Weather>(Codec<T> codec) {
    public static final ResourceKey<Registry<WeatherType<?>>> KEY = ResourceKey.<WeatherType<?>>createRegistryKey(new ResourceLocation(BetterWeather.MOD_ID, "weather"));
    public static final DeferredRegister<WeatherType<?>> DEFERRED_REGISTER = DeferredRegister.create(KEY, BetterWeather.MOD_ID);
    public static final Supplier<IForgeRegistry<WeatherType<?>>> REGISTRY = DEFERRED_REGISTER.makeRegistry(() -> new RegistryBuilder<WeatherType<?>>().allowModification());
    public static final Codec<Weather> CODEC = REGISTRY.get().getCodec().dispatch(Weather::type, WeatherType::codec);

    public static final RegistryObject<WeatherType<Weather>> BASIC = register("basic", Weather.CODEC_IMPL);
    public static final RegistryObject<WeatherType<Snow>> BLIZZARD = register("blizzard", Snow.CODEC);
    public static final RegistryObject<WeatherType<Sunny>> NONE = register("sunny", Sunny.CODEC);

    public static <T extends Weather> RegistryObject<WeatherType<T>> register(String name, Codec<T> codec) {
        return DEFERRED_REGISTER.register(name, () -> new WeatherType<>(codec));
    }

    public static void register(IEventBus bus) {
        DEFERRED_REGISTER.register(bus);
    }
}
