package corgitaco.betterweather.weather.event;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public interface EntityCheck {
    Codec<EntityCheck> ENTITY_CHECK_CODEC = Codec.STRING.xmap(s -> {
        if (s.startsWith("@")) {
            return new CategoryCheck(MobCategory.byName(s.substring(1)));
        } else {
            var isTag = s.startsWith("#");
            var location = isTag ? new ResourceLocation(s.substring(1)) : new ResourceLocation(s);

            if (isTag) return new TagCheck(TagKey.create(Registries.ENTITY_TYPE, location));
            else return new TypeCheck(ResourceKey.create(Registries.ENTITY_TYPE, location));
        }
    }, Object::toString);

    boolean isValid(Entity entity);

    record TagCheck(TagKey<EntityType<?>> tag) implements EntityCheck {

        @Override
        public boolean isValid(Entity entity) {
            return entity.getType().builtInRegistryHolder().is(tag);
        }

        @Override
        public String toString() {
            return "#" + tag.location();
        }
    }

    record TypeCheck(ResourceKey<EntityType<?>> type) implements EntityCheck {

        @Override
        public boolean isValid(Entity entity) {
            return entity.getType().builtInRegistryHolder().is(type);
        }

        @Override
        public String toString() {
            return type.location().toString();
        }
    }

    record CategoryCheck(MobCategory mobCategory) implements EntityCheck {

        @Override
        public boolean isValid(Entity entity) {
            return entity.getType().getCategory() == mobCategory;
        }

        @Override
        public String toString() {
            return "@" + mobCategory.getSerializedName();
        }
    }
}
