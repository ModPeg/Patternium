package com.patternium.mod.crafting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkCrafter {

    private enum State { IDLE, FILL_GRID, WAIT_RESULT, CRAFT }

    private State state = State.IDLE;
    private int tickCounter = 0;
    private int checkRetries = 0;
    private int fillRetries = 0;

    private static final int TICK_DELAY = 2;
    private static final int MAX_CHECK_RETRIES = 10;
    private static final int MAX_FILL_RETRIES = 5;

    private final List<RecipeSlot> recipeSlots = new ArrayList<>();
    private int gridStart = 0;
    private int gridEnd = 0;

    private static final BulkCrafter INSTANCE = new BulkCrafter();

    private BulkCrafter() {}

    public static BulkCrafter getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public void start(AbstractContainerMenu handler, LocalPlayer player) {
        if (!(handler instanceof AbstractCraftingMenu craftingHandler)) return;

        gridStart = craftingHandler.getResultSlot().index + 1;
        gridEnd = gridStart + CraftingHelper.getCraftingSlotCount(handler);

        recipeSlots.clear();
        for (int i = gridStart; i < gridEnd; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                recipeSlots.add(new RecipeSlot(i - gridStart, id));
            } else {
                recipeSlots.add(new RecipeSlot(i - gridStart, ""));
            }
        }

        state = State.CRAFT;
        tickCounter = 0;
        checkRetries = 0;
    }

    public void stop() {
        state = State.IDLE;
        recipeSlots.clear();
    }

    public void tick() {
        if (state == State.IDLE) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            stop();
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        if (!(handler instanceof AbstractCraftingMenu)) {
            stop();
            return;
        }

        int resultIdx = CraftingHelper.getResultSlotIndex(handler);
        if (resultIdx < 0) {
            stop();
            return;
        }

        tickCounter++;
        if (tickCounter < TICK_DELAY) return;
        tickCounter = 0;

        LocalPlayer player = client.player;

        switch (state) {
            case FILL_GRID -> doFillGrid(handler, player, resultIdx);
            case WAIT_RESULT -> doWaitResult(handler, resultIdx);
            case CRAFT -> doCraft(handler, player, resultIdx);
            default -> {}
        }
    }

    private void doFillGrid(AbstractContainerMenu handler, LocalPlayer player, int resultIdx) {
        Map<String, List<Integer>> itemToSlots = new HashMap<>();
        for (int i = 0; i < recipeSlots.size(); i++) {
            RecipeSlot rs = recipeSlots.get(i);
            if (!rs.itemId.isEmpty()) {
                itemToSlots.computeIfAbsent(rs.itemId, k -> new ArrayList<>()).add(i);
            }
        }

        for (int i = 0; i < recipeSlots.size(); i++) {
            int slotIdx = gridStart + i;
            if (slotIdx >= handler.slots.size() || slotIdx >= gridEnd) continue;

            RecipeSlot rs = recipeSlots.get(i);
            ItemStack current = handler.getSlot(slotIdx).getItem();
            if (current.isEmpty()) continue;

            if (!rs.itemId.isEmpty() && itemsMatch(current, rs.itemId)) continue;
            clickSlot(handler, slotIdx, 0, ContainerInput.QUICK_MOVE, player);
        }

        for (Map.Entry<String, List<Integer>> entry : itemToSlots.entrySet()) {
            String itemId = entry.getKey();
            List<Integer> slotIndices = entry.getValue();

            int totalAvailable = 0;
            List<Integer> inventorySlots = new ArrayList<>();
            for (int invSlot = 0; invSlot < handler.slots.size(); invSlot++) {
                if (invSlot == resultIdx || (invSlot >= gridStart && invSlot < gridEnd)) continue;
                ItemStack stack = handler.getSlot(invSlot).getItem();
                if (!stack.isEmpty() && itemsMatch(stack, itemId)) {
                    totalAvailable += stack.getCount();
                    inventorySlots.add(invSlot);
                }
            }

            if (totalAvailable <= 0) continue;

            int perSlot = totalAvailable / slotIndices.size();
            if (perSlot <= 0) perSlot = 1;

            int maxStack = 64;
            if (!inventorySlots.isEmpty()) {
                ItemStack sample = handler.getSlot(inventorySlots.get(0)).getItem();
                maxStack = sample.getMaxStackSize();
            }
            perSlot = Math.min(perSlot, maxStack);

            for (int slotIdx : slotIndices) {
                int targetSlot = gridStart + slotIdx;
                if (targetSlot >= handler.slots.size() || targetSlot >= gridEnd) continue;

                ItemStack current = handler.getSlot(targetSlot).getItem();
                if (!current.isEmpty() && itemsMatch(current, itemId) && current.getCount() >= perSlot) continue;

                int needed = perSlot;
                if (!current.isEmpty() && itemsMatch(current, itemId)) {
                    needed -= current.getCount();
                }
                if (needed <= 0) continue;

                for (int invSlot : inventorySlots) {
                    if (needed <= 0) break;
                    ItemStack invStack = handler.getSlot(invSlot).getItem();
                    if (invStack.isEmpty() || !itemsMatch(invStack, itemId)) continue;

                    clickSlot(handler, invSlot, 0, ContainerInput.PICKUP, player);
                    int toPlace = Math.min(needed, handler.getCarried().getCount());
                    for (int p = 0; p < toPlace; p++) {
                        if (handler.getCarried().isEmpty()) break;
                        clickSlot(handler, targetSlot, 1, ContainerInput.PICKUP, player);
                        needed--;
                    }
                    if (!handler.getCarried().isEmpty()) {
                        clickSlot(handler, invSlot, 0, ContainerInput.PICKUP, player);
                    }
                }
            }
        }

        for (int i = 0; i < recipeSlots.size(); i++) {
            RecipeSlot rs = recipeSlots.get(i);
            if (rs.itemId.isEmpty()) continue;

            int targetSlot = gridStart + i;
            if (targetSlot >= handler.slots.size() || targetSlot >= gridEnd) continue;

            ItemStack stack = handler.getSlot(targetSlot).getItem();
            if (stack.isEmpty() || !itemsMatch(stack, rs.itemId)) {
                if (++fillRetries >= MAX_FILL_RETRIES) {
                    stop();
                }
                return;
            }
        }

        state = State.WAIT_RESULT;
        checkRetries = 0;
        fillRetries = 0;
    }

    private void doWaitResult(AbstractContainerMenu handler, int resultIdx) {
        ItemStack resultStack = handler.getSlot(resultIdx).getItem();
        if (!resultStack.isEmpty() && resultStack.getCount() > 0) {
            state = State.CRAFT;
            checkRetries = 0;
        } else if (++checkRetries >= MAX_CHECK_RETRIES) {
            stop();
        }
    }

    private void doCraft(AbstractContainerMenu handler, LocalPlayer player, int resultIdx) {
        clickSlot(handler, resultIdx, 0, ContainerInput.QUICK_MOVE, player);

        ItemStack remaining = handler.getSlot(resultIdx).getItem();
        if (!remaining.isEmpty() && remaining.getCount() > 0) {
            clickSlot(handler, resultIdx, 1, ContainerInput.THROW, player);
        }

        state = State.FILL_GRID;
    }

    private static void clickSlot(AbstractContainerMenu handler, int slot, int button,
                                  ContainerInput action, LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null) {
            client.gameMode.handleContainerInput(handler.containerId, slot, button, action, player);
        }
    }

    private static boolean itemsMatch(ItemStack stack, String itemId) {
        if (stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId);
    }

    private record RecipeSlot(int index, String itemId) {}
}
