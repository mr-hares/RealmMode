package ru.mr_hares.realmMode.utils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiSwitchFlag {
    private static final Set<UUID> SWITCHING = ConcurrentHashMap.newKeySet();

    private GuiSwitchFlag() {}

    public static void mark(UUID uuid)    { SWITCHING.add(uuid); }
    public static boolean isMarked(UUID u){ return SWITCHING.contains(u); }
    public static void clear(UUID uuid)   { SWITCHING.remove(uuid); }
}