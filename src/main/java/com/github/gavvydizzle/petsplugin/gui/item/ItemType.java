package com.github.gavvydizzle.petsplugin.gui.item;

import org.jetbrains.annotations.Nullable;

public enum ItemType {
    BACK,
    ITEM,
    LINK,
    PET;

    @Nullable
    public static ItemType get(@Nullable String str) {
        if (str == null) return null;

        for (ItemType itemType : ItemType.values()) {
            if (str.equalsIgnoreCase(itemType.name())) return itemType;
        }
        return null;
    }
}
