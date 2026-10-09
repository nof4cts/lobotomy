package com.gojo.mod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GojoMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GojoMod.MODID);

    // name, cursed energy cost, cooldown (ticks), tooltip, technique
    public static final RegistryObject<Item> INFINITY = technique("infinity", 0, 10,
            "Toggle Infinity: attacks slow to a stop before they reach you.", Techniques::infinity);
    public static final RegistryObject<Item> BLUE = technique("blue", 100, 40,
            "Lapse: Blue - pulls everything toward a point and crushes it.", Techniques::blue);
    public static final RegistryObject<Item> RED = technique("red", 150, 60,
            "Reversal: Red - a violent repelling blast.", Techniques::red);
    public static final RegistryObject<Item> PURPLE = technique("purple", 400, 200,
            "Hollow Technique: Purple - erases everything in its path.", Techniques::purple);
    public static final RegistryObject<Item> UNLIMITED_VOID = technique("unlimited_void", 600, 600,
            "Domain Expansion - paralyzes everyone around you.", Techniques::unlimitedVoid);
    public static final RegistryObject<Item> TELEPORT = technique("teleport", 60, 10,
            "Compress space and blink to where you are looking.", Techniques::teleport);
    public static final RegistryObject<Item> REVERSE_CURSED_TECHNIQUE = technique("reverse_cursed_technique", 200, 200,
            "Fully heal and purge harmful effects.", Techniques::reverseCursedTechnique);
    public static final RegistryObject<Item> SIX_EYES = technique("six_eyes", 50, 200,
            "Use: sense every living thing nearby. Carry: triples cursed energy regen.", Techniques::sixEyes);
    public static final RegistryObject<Item> CURTAIN = technique("curtain", 100, 400,
            "Drop a Curtain: storm, darkness for others, and you vanish.", Techniques::curtain);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("gojo", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gojo"))
            .icon(() -> new ItemStack(SIX_EYES.get()))
            .displayItems((params, output) -> ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    private ModItems() {
    }

    private static RegistryObject<Item> technique(String name, int cost, int cooldownTicks, String description,
                                                  TechniqueItem.Technique technique) {
        return ITEMS.register(name, () -> new TechniqueItem(
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(),
                cost, cooldownTicks, description, technique));
    }
}
