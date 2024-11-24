package corgitaco.betterweather.data.storage;

import corgitaco.betterweather.BetterWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class WeatherEventSavedData extends SavedData {
    public static String DATA_NAME = new ResourceLocation(BetterWeather.MOD_ID, "weather_event_data").toString();
    private boolean isWeatherForced;
    private String event;

    public WeatherEventSavedData() {
        super();
    }

    private static WeatherEventSavedData clientCache = new WeatherEventSavedData();
    private static ClientLevel worldCache = null;

    public static WeatherEventSavedData get(Level world) {
        if (!(world instanceof ServerLevel)) {
            if (worldCache != world) {
                worldCache = (ClientLevel) world;
                clientCache = new WeatherEventSavedData();
            }
            return clientCache;
        }
        DimensionDataStorage data = ((ServerLevel) world).getDataStorage();
        WeatherEventSavedData weatherData = data.computeIfAbsent(compoundTag -> new WeatherEventSavedData(), WeatherEventSavedData::new, DATA_NAME);

        if (weatherData == null) {
            weatherData = new WeatherEventSavedData();
            data.set(DATA_NAME, weatherData);
        }

        return weatherData;
    }

    @Override
    public void save(@NotNull File dataFile) {
        if(!dataFile.exists()) {
            dataFile = new File(dataFile.getAbsolutePath());
        }

        super.save(dataFile);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compound) {
        compound.putString("Event", event);
        compound.putBoolean("Forced", isWeatherForced);
        return compound;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
        setDirty();
    }

    public boolean isWeatherForced() {
        return isWeatherForced;
    }

    public void setWeatherForced(boolean weatherForced) {
        isWeatherForced = weatherForced;
        setDirty();
    }
}