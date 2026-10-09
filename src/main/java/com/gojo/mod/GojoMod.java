package com.gojo.mod;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GojoMod.MODID)
public class GojoMod {
    public static final String MODID = "gojo";

    public GojoMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        // GojoEvents registers itself on the Forge bus via @Mod.EventBusSubscriber.
    }
}
