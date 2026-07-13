package com.patternium.mod.crafting;

import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class CraftingHelper {
    private CraftingHelper() {}

    public static int getResultSlotIndex(AbstractContainerMenu handler) {
        return handler instanceof AbstractCraftingMenu c
                ? c.getResultSlot().index : -1;
    }

    public static int getCraftingSlotCount(AbstractContainerMenu handler) {
        return handler instanceof AbstractCraftingMenu c
                ? c.getInputGridSlots().size() : 0;
    }
}
