package ru.liko.pjmbasemod.client.compat;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional integration with WarBornGuard (client-side).
 * Uses reflection to avoid hard dependency: Pjmbasemod can run without WarBornGuard present.
 */
public final class WarBornGuardCompat {
    // Matches ru.liko.warbornguard.network.packet.SyncAdminStatusPacket flags
    public static final int FLAG_VANISH = 1;
    public static final int FLAG_ESP = 2;

    private static boolean init = false;
    private static Method getFlagsMethod;

    private WarBornGuardCompat() {
    }

    public static int getFlags(UUID playerId) {
        ensureInit();
        if (getFlagsMethod == null || playerId == null) return 0;
        try {
            Object res = getFlagsMethod.invoke(null, playerId);
            return res instanceof Integer ? (Integer) res : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void ensureInit() {
        if (init) return;
        init = true;
        try {
            Class<?> cache = Class.forName("ru.liko.warbornguard.client.AdminStatusClientCache");
            getFlagsMethod = cache.getMethod("getFlags", UUID.class);
        } catch (Throwable ignored) {
            getFlagsMethod = null;
        }
    }
}


