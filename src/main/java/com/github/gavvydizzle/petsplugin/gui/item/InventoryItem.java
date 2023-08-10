package com.github.gavvydizzle.petsplugin.gui.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InventoryItem {

    private final ItemType itemType;
    @Nullable private final ItemStack itemStack;
    @Nullable private final String extra;

    public InventoryItem(ItemType itemType, @Nullable ItemStack itemStack, @Nullable String extra) {
        this.itemType = itemType;
        this.itemStack = itemStack;
        this.extra = extra;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public @Nullable ItemStack getItemStack() {
        return itemStack;
    }

    public @Nullable String getExtra() {
        return extra;
    }
}
