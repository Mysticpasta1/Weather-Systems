package corgitaco.betterweather.util;

import com.mojang.serialization.Lifecycle;
import corgitaco.betterweather.mixin.access.BiomeAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Used to allow either server or world specific biome objects to function as keys to return the same common biome for the given world since each world can contain a unique biome registry.
 */
public class CommonKeyMutableRegistry extends MappedRegistry<Biome> {
    private final Map<Biome, Biome> storage = new IdentityHashMap<>();

    // Registry is the Server registry.
    public CommonKeyMutableRegistry(WritableRegistry<Biome> registry) {
        super(registry.key(), registry.registryLifecycle(), false);
        registry.entrySet().forEach(entry -> {
            Biome biome1 = entry.getValue();
            Holder<Biome> biome2 = registerMapping(
                    registry.getId(biome1),
                    entry.getKey(),
                    shallow(biome1),
                    registry.lifecycle(biome1)
            );

            ResourceLocation name = requireNonNull(registry.getKey(biome1), "Invalid Biome registry name.");

            storage.put(biome1, biome2.value());
        });
    }

    // Creates a shallow copy of a biome.
    private static Biome shallow(Biome biome) {
        @SuppressWarnings("ConstantConditions") // Mixins are used.
        Biome.ClimateSettings climate = ((BiomeAccess) (Object) biome).getClimateSettings();

        return BiomeAccess.create(
                climate,
                biome.getSpecialEffects(),
                biome.getGenerationSettings(),
                biome.getMobSettings()
        );
    }

    // Overrides.
    @Override
    public ResourceLocation getKey(@NotNull Biome biome) {
        return super.getKey(get(biome));
    }

    @Override
    public @NotNull Optional<ResourceKey<Biome>> getResourceKey(@NotNull Biome biome) {
        return super.getResourceKey(get(biome));
    }

    @Override
    public int getId(@Nullable Biome biome) {
        return super.getId(get(biome));
    }

    @Override
    public @NotNull Lifecycle lifecycle(@NotNull Biome biome) {
        return super.lifecycle(get(biome));
    }

    // Get the mapped biome, otherwise the biome itself.
    @Contract("null -> param1")
    private Biome get(Biome biome) {
        return storage.getOrDefault(biome, biome);
    }
}
