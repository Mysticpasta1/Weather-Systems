package corgitaco.betterweather.datageb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.api.BetterWeatherRegistry;
import corgitaco.betterweather.api.weather.Weather;
import corgitaco.betterweather.weather.event.DefaultEvents;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class WeatherProvider implements DataProvider {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

	private final PackOutput.PathProvider decayPatternPathResolver;

	public WeatherProvider(PackOutput output) {
		this.decayPatternPathResolver = output.createPathProvider(PackOutput.Target.DATA_PACK, "weather");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
//		Set<ResourceLocation> generatedDecayPatterns = Sets.newHashSet();


		List<CompletableFuture<?>> list = new ArrayList<>();


		BiConsumer<ResourceLocation, JsonObject> consumer = (resourceLocation, json) -> {
			Path outputPath = decayPatternPathResolver.json(resourceLocation);
			list.add(DataProvider.saveStable(cache, json, outputPath));
		};

		generateWeather(consumer);

		return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
	}

	private void generateWeather(BiConsumer<ResourceLocation, JsonObject> consumer) {
		DefaultEvents.init();

		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "partly_cloudy"), DefaultEvents.PARTLY_CLOUDY);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "acid_drizzle"), DefaultEvents.ACID_DRIZZLE);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "dusting"), DefaultEvents.DUSTING);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "drizzle"), DefaultEvents.DRIZZLE);

		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "cloudy"), DefaultEvents.CLOUDY);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "acid_rain"), DefaultEvents.ACID_RAIN);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "snow"), DefaultEvents.SNOW);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "rain"), DefaultEvents.RAIN);

		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "overcast"), DefaultEvents.OVERCAST);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "acid_downpour"), DefaultEvents.ACID_DOWNPOUR);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "blizzard"), DefaultEvents.BLIZZARD);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "downpour"), DefaultEvents.DOWNPOUR);

		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "heat_lightning"), DefaultEvents.HEAT_LIGHTNING);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "acid_lightning"), DefaultEvents.ACID_LIGHTNING);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "thunder_blizzard"), DefaultEvents.THUNDER_BLIZZARD);
		createWeather(consumer, new ResourceLocation(BetterWeather.MOD_ID, "lightning"), DefaultEvents.LIGHTNING);
	}

	private void createWeather(BiConsumer<ResourceLocation, JsonObject> consumer, ResourceLocation id, Weather weather) {
		try {

			var json = Weather.CODEC.encode(weather, JsonOps.INSTANCE, new JsonObject());

		consumer.accept(id, json.get().orThrow().getAsJsonObject());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return "Weather";
	}
}
