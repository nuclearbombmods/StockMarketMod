package com.stockmarketmod.sound;

import com.stockmarketmod.StockMarketMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StockMarketMod.MODID);

    public static final RegistryObject<SoundEvent> TRADE_SUCCESS = SOUND_EVENTS.register("trade_success",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StockMarketMod.MODID, "trade_success")));
    
    public static final RegistryObject<SoundEvent> TRADE_FAIL = SOUND_EVENTS.register("trade_fail",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StockMarketMod.MODID, "trade_fail")));
    
    public static final RegistryObject<SoundEvent> MARKET_UPDATE = SOUND_EVENTS.register("market_update",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StockMarketMod.MODID, "market_update")));
} 