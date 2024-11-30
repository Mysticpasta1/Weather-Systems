package corgitaco.betterweather.weather;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import corgitaco.betterweather.api.weather.Weather;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class WeatherLoader implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static WeatherLoader INSTANCE = new WeatherLoader();
    private final Map<ResourceLocation, Weather> weatherMap = new HashMap<>();

    public static WeatherLoader getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, Weather> getWeathers() {
        return weatherMap;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        weatherMap.clear();

        manager.listResources("weather", resourceLocation -> resourceLocation.getPath().endsWith(".json")).forEach((resourceLocation, resource) -> {
            try {
                weatherMap.put(resourceLocation, JsonOps.INSTANCE.withDecoder(Weather.CODEC).apply(GSON.fromJson(new InputStreamReader(resource.open()), JsonElement.class)).getOrThrow(false, System.out::println).getFirst());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
