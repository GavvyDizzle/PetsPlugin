package com.github.gavvydizzle.petsplugin.gui.view;

import com.github.gavvydizzle.petsplugin.player.LoadedPlayer;
import com.github.mittenmc.serverutils.gui.ClickableMenu;

import java.util.UUID;

public record AdminViewer(UUID uuid, LoadedPlayer loadedPlayer, ClickableMenu clickableMenu) {}
