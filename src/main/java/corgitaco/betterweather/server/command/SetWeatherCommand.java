package corgitaco.betterweather.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.helpers.BetterWeatherWorldData;
import corgitaco.betterweather.weather.BWWeatherEventContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;
import java.util.List;

public class SetWeatherCommand {

    public static final String WEATHER_NOT_ENABLED = "null";
    public static final List<String> LENGTH_SUGGESTIONS = Arrays.asList(
            "1200", // a minute
            "6000", // 5 minutes
            "12000", // 10 minutes
            "36000" // 30 minutes
    );

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("setweather").then(
                Commands.argument("weather", ResourceLocationArgument.id())
                        .suggests((ctx, sb) -> {
                            BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) ctx.getSource().getLevel()).getWeatherEventContext();
                            return SharedSuggestionProvider.suggest(weatherEventContext != null ? weatherEventContext.getWeatherEvents().keySet().stream().map(a -> a.toString()) : Arrays.stream(new String[]{WEATHER_NOT_ENABLED}), sb);
                        }).executes(cs -> betterWeatherSetWeather(cs.getSource(), cs.getArgument("weather", ResourceLocation.class),
                                12000)) // Default length to 10 minutes.
                        .then(
                                Commands.argument("length", IntegerArgumentType.integer())
                                        .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(LENGTH_SUGGESTIONS.stream(), sb))
                                        .executes((cs) -> betterWeatherSetWeather(cs.getSource(), cs.getArgument("weather", ResourceLocation.class),
                                                cs.getArgument("length", int.class)))
                        )
        );
    }

    public static int betterWeatherSetWeather(CommandSourceStack source, ResourceLocation weatherKey, int length) {
        if (weatherKey.equals(WEATHER_NOT_ENABLED)) {
            source.sendFailure(Component.translatable("commands.bw.setweather.no.weather.for.world"));
            return 0;
        }

        ServerLevel world = source.getLevel();
        BWWeatherEventContext weatherEventContext = ((BetterWeatherWorldData) world).getWeatherEventContext();

        if (weatherEventContext != null) {
            if (weatherEventContext.getWeatherEvents().containsKey(weatherKey)) {
                source.sendSuccess(() -> weatherEventContext.weatherForcer(weatherKey, length, world).successTranslationTextComponent(weatherKey.toLanguageKey()), true);
            } else {
                source.sendFailure(Component.translatable("commands.bw.setweather.fail.no_weather_event", weatherKey));
                return 0;
            }
        }
        return 1;
    }
}
