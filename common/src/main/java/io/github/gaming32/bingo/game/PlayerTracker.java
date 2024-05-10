package io.github.gaming32.bingo.game;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;

public class PlayerTracker {
    private static PlayerTracker INSTANCE;
    public HashMap<ServerPlayer, ServerPlayer> trackedPlayers;
    public boolean enabled = false;

    private PlayerTracker() {
        trackedPlayers = new HashMap<>();
    }

    public static PlayerTracker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerTracker();
        }
        return INSTANCE;
    }
}
