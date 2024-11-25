package corgitaco.betterweather.weather;

import com.mojang.serialization.Codec;
import corgitaco.betterweather.weather.event.EntityCheck;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Predicate;

public interface BiomeCheck {
    Codec<BiomeCheck> ENTITY_CHECK_CODEC = Codec.STRING.xmap(s -> {
        if(s.equals("all")) return AlwaysTrueCheck.INSTANCE;
        else {
            var isTag = s.startsWith("#");
            var location = isTag ? new ResourceLocation(s.substring(1)) : new ResourceLocation(s);

            if (isTag) return new TagCheck(TagKey.create(Registries.BIOME, location));
            else return new TypeCheck(ResourceKey.create(Registries.BIOME, location));
        }
    }, Object::toString);

    public boolean isValid(Registry<Biome> registry, Biome biome);

    record TagCheck(TagKey<Biome> tag) implements BiomeCheck {

        @Override
        public boolean isValid(Registry<Biome> registry, Biome biome) {
            return registry.getResourceKey(biome).map(registry::getHolderOrThrow).filter(biomeReference -> registry.getTag(tag).filter(holders -> holders.contains(biomeReference)).isPresent()).isPresent();
        }

        @Override
        public String toString() {
            return "#" + tag.location();
        }
    }

    record TypeCheck(ResourceKey<Biome> key) implements BiomeCheck {

        @Override
        public boolean isValid(Registry<Biome> registry, Biome biome) {
            return registry.getResourceKey(biome).filter(key::equals).isPresent();
        }

        @Override
        public String toString() {
            return key.location().toString();
        }
    }

    record AlwaysTrueCheck() implements BiomeCheck {
        public static final AlwaysTrueCheck INSTANCE = new AlwaysTrueCheck();

        @Override
        public boolean isValid(Registry<Biome> registry, Biome biome) {
            return true;
        }


            @Override
        public String toString() {
            return "all";
        }
    }
}
