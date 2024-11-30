package corgitaco.betterweather.datageb;

import net.minecraftforge.data.event.GatherDataEvent;

public class DataEvent {
    public static void onData(GatherDataEvent event) {
        var pack = event.getGenerator().getPackOutput();

        event.getGenerator().addProvider(true, new WeatherProvider(pack));
    }
}
