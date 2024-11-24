package corgitaco.betterweather.util;

import corgitaco.betterweather.BetterWeather;
import corgitaco.betterweather.util.client.ColorUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

@SuppressWarnings("deprecation")
public class BetterWeatherUtil {

    public static boolean filterRegistryID(ResourceLocation id, Registry<?> registry, String registryTypeName) {
        if (registry.keySet().contains(id))
            return true;
        else {
            if (!id.toString().contains("modid:dummymob")) {
                BetterWeather.LOGGER.error("\"" + id.toString() + "\" was not a registryID in the " + registryTypeName + "! Skipping entry...");
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <I, O> O unsafeCast(I obj) {
        return (O) obj;
    }


    public static int modifiedColorValue(int original, int target, double blendStrength) {
        return (int) Mth.lerp(blendStrength, original, target);
    }


    public static int transformFlfoatColor(Vector3d floatColor) {
        return ColorUtil.pack((int) (floatColor.x * 255), (int) (floatColor.y * 255), (int) (floatColor.z * 255));
    }

}