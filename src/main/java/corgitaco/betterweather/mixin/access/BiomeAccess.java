package corgitaco.betterweather.mixin.access;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface BiomeAccess {

    @Accessor
    Biome.ClimateSettings getClimateSettings();

    @Invoker("<init>")
    static Biome create(Biome.ClimateSettings climate, BiomeSpecialEffects specialEffects, BiomeGenerationSettings biomeGenerationSettings, MobSpawnSettings mobSpawnSettings) {
        throw new Error("Mixin did not apply");
    }
}
