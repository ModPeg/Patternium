package com.patternium.mod.mixin;

import com.patternium.mod.crafting.BulkCrafter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractCraftingScreenHandlerMixin {

    @Inject(method = "onMouseClickAction(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, ContainerInput actionType, CallbackInfo ci) {
        if (slot == null) return;
        if (actionType != ContainerInput.QUICK_MOVE) return;

        Window window = Minecraft.getInstance().getWindow();
        boolean ctrl = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                     || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!ctrl || !shift) return;

        AbstractContainerMenu handler = ((AbstractContainerScreen<?>) (Object) this).getMenu();
        if (!(handler instanceof AbstractCraftingMenu craftingHandler)) return;

        int resultIdx = craftingHandler.getResultSlot().index;
        if (slot.index != resultIdx) return;

        ci.cancel();

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            BulkCrafter.getInstance().start(handler, client.player);
        }
    }
}
