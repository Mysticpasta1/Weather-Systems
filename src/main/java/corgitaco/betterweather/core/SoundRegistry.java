package corgitaco.betterweather.core;

import corgitaco.betterweather.BetterWeather;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static SoundEvent BLIZZARD_LOOP1 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop1"));
    public static SoundEvent BLIZZARD_LOOP2 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop2"));
    public static SoundEvent BLIZZARD_LOOP3 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop3"));
    public static SoundEvent BLIZZARD_LOOP4 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop4"));
    public static SoundEvent BLIZZARD_LOOP5 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop5"));
    public static SoundEvent BLIZZARD_LOOP6 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop6"));
    public static SoundEvent BLIZZARD_LOOP7 = SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterWeather.MOD_ID, "blizzard_loop7"));

    public static final DeferredRegister<SoundEvent> SOUND_EVENT_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BetterWeather.MOD_ID);

    public static void bwRegisterSounds(EventBus eventBus) {
        SOUND_EVENT_DEFERRED_REGISTER.register(eventBus);
    }

    public static final RegistryObject<SoundEvent> BLIZZARD1 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop1", () -> BLIZZARD_LOOP1);
    public static final RegistryObject<SoundEvent> BLIZZARD2 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop2", () -> BLIZZARD_LOOP2);
    public static final RegistryObject<SoundEvent> BLIZZARD3 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop3", () -> BLIZZARD_LOOP3);
    public static final RegistryObject<SoundEvent> BLIZZARD4 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop4", () -> BLIZZARD_LOOP4);
    public static final RegistryObject<SoundEvent> BLIZZARD5 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop5", () -> BLIZZARD_LOOP5);
    public static final RegistryObject<SoundEvent> BLIZZARD6 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop6", () -> BLIZZARD_LOOP6);
    public static final RegistryObject<SoundEvent> BLIZZARD7 = SOUND_EVENT_DEFERRED_REGISTER.register("blizzard_loop7", () -> BLIZZARD_LOOP7);



}