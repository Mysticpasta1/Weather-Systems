package corgitaco.betterweather.weather.event;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import corgitaco.betterweather.api.client.ColorSettings;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.core.SoundRegistry;
import corgitaco.betterweather.util.Textures;
import corgitaco.betterweather.util.client.ColorUtil;
import corgitaco.betterweather.weather.event.client.settings.*;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEvents {
    public static final ColorSettings THUNDER_COLORS = new ColorSettings(Integer.MAX_VALUE, 0.1, Integer.MAX_VALUE, 0.0, ColorUtil.DEFAULT_THUNDER_SKY, 1.0F, ColorUtil.DEFAULT_THUNDER_FOG, 1.0F, ColorUtil.DEFAULT_THUNDER_CLOUDS, 1.0F);
    public static final ColorSettings RAIN_COLORS = new ColorSettings(Integer.MAX_VALUE, 0.1, Integer.MAX_VALUE, 0.0, ColorUtil.DEFAULT_RAIN_SKY, 1.0F, ColorUtil.DEFAULT_RAIN_FOG, 1.0F, ColorUtil.DEFAULT_RAIN_CLOUDS, 1.0F);

    public static final String DEFAULT_BIOME_CONDITION = "!#DESERT#SAVANNA#NETHER#THEEND";

    public static final boolean MODIFY_TEMPERATURE = false;

    public static final Map<Block, Block> DEFAULT_DECAYER = Util.make(new HashMap<>(), (map) -> {
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block.defaultBlockState().is(BlockTags.LEAVES) || block.defaultBlockState().is(BlockTags.CROPS)) {
                map.put(block, Blocks.AIR);
            }
        }

        map.put(Blocks.GRASS_BLOCK, Blocks.DIRT);
        map.put(Blocks.PODZOL, Blocks.DIRT);
        map.put(Blocks.MYCELIUM, Blocks.DIRT);
    });

    public static final List<Pair<EntityCheck, Float>> DEFAULT_ENTITY_DAMAGE = Util.make(new ArrayList<>(), (map) -> {
        map.add(Pair.of(new EntityCheck.CategoryCheck(MobCategory.MONSTER), 0.5F));
        map.add(Pair.of(new EntityCheck.CategoryCheck(MobCategory.AMBIENT), 0.5F));
        map.add(Pair.of(new EntityCheck.CategoryCheck(MobCategory.CREATURE), 0.5F));
        map.add(Pair.of(new EntityCheck.TypeCheck(EntityType.PLAYER.builtInRegistryHolder().key()), 0.5F));
    });

    public static final Weather ACID_RAIN_DEFAULT = new Weather(new AcidRainClientSettings(RAIN_COLORS, 0.0F, -1.0F, true, Textures.ACID_RAIN_LOCATION, Textures.SNOW_LOCATION, true), new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.25D, !MODIFY_TEMPERATURE ? 0.0 : -0.1, 0.1, false, 0), new Weather.DecaySettings(150, 100, DEFAULT_DECAYER, DEFAULT_ENTITY_DAMAGE));
    public static final Weather ACID_RAIN_DEFAULT_THUNDERING = new Weather(new AcidRainClientSettings(THUNDER_COLORS, 0.0F, -1.0F, true, Textures.ACID_RAIN_LOCATION, Textures.SNOW_LOCATION, true), new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.125D, !MODIFY_TEMPERATURE ? 0.0 : -0.1, 0.1, true, 100000), new Weather.DecaySettings(150, 100, DEFAULT_DECAYER, DEFAULT_ENTITY_DAMAGE));
    public static final Weather RAIN_DEFAULT = new Weather(new RainClientSettings(RAIN_COLORS, 0.0F, -1.0F, true, Textures.RAIN_LOCATION, Textures.SNOW_LOCATION), new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.7D, !MODIFY_TEMPERATURE ? 0.0 : -0.1, 0.1, false, 0), Weather.DecaySettings.NONE);
    public static final Weather RAIN_DEFAULT_THUNDERING = new Weather(new RainClientSettings(THUNDER_COLORS, 0.0F, -1.0F, true, Textures.RAIN_LOCATION, Textures.SNOW_LOCATION), new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.3D, !MODIFY_TEMPERATURE ? 0.0 : -0.5, 0.1, true, 100000), Weather.DecaySettings.NONE);
    public static final Snow SNOW_DEFAULT = new Snow(
            new BlizzardClientSettings(new ColorSettings(Integer.MAX_VALUE, 0.0, Integer.MAX_VALUE, 0.0), 0.0F, 0.2F, false, Textures.SNOW_LOCATION, Holder.direct(SoundRegistry.BLIZZARD_LOOP2), 0.6F, 0.6F),
            new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.1D, !MODIFY_TEMPERATURE ? 0.0 : -0.5, 0.1, false, 0, Util.make(new ArrayList<>(), ((stringListHashMap) -> stringListHashMap.add(new Pair<>(new EntityCheck.TypeCheck(EntityType.PLAYER.builtInRegistryHolder().key()), ImmutableList.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN))))))),
            Weather.DecaySettings.NONE, 2, 10, Blocks.SNOW, true, true);

    public static final Snow SNOW_DEFAULT_THUNDERING = new Snow(
            new BlizzardClientSettings(new ColorSettings(Integer.MAX_VALUE, 0.0, Integer.MAX_VALUE, 0.0), 0.0F, 0.2F, false, Textures.SNOW_LOCATION, Holder.direct(SoundRegistry.BLIZZARD_LOOP2), 0.6F, 0.6F),
            new Weather.BasicSettings(DEFAULT_BIOME_CONDITION, 0.05D, !MODIFY_TEMPERATURE ? 0.0 : -0.5, 0.1, true, 100000, Util.make(new ArrayList<>(), ((stringListHashMap) -> stringListHashMap.add(new Pair<>(new EntityCheck.TypeCheck(EntityType.PLAYER.builtInRegistryHolder().key()), ImmutableList.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN))))))),
            Weather.DecaySettings.NONE, 2, 10, Blocks.SNOW, true, true);

    public static final Weather CLOUDY_DEFAULT = new Weather(new CloudyClientSettings(RAIN_COLORS, 0.0F, -1.0F, true), new Weather.BasicSettings("ALL", 0.7D, !MODIFY_TEMPERATURE ? 0.0 : -0.05, 0.07, false, 0), Weather.DecaySettings.NONE);

    public static final Weather CLOUDY_DEFAULT_THUNDERING = new Weather(new CloudyClientSettings(THUNDER_COLORS, 0.0F, -0.09F, true), new Weather.BasicSettings("ALL", 0.1D, !MODIFY_TEMPERATURE ? 0.0 :-0.05, 0.07, true, 100000), Weather.DecaySettings.NONE);
    public static final Sunny DEFAULT_SUNNY = new Sunny(new SunnyClientSettings(new ColorSettings(Integer.MAX_VALUE, 0.0, Integer.MAX_VALUE, 0.0)));

}
