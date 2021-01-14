package fr.modcraftmc.forge.utils;

import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ModcraftUtils {

    public static boolean isPrimaryThread() {
        // Tuinity start
        final Thread currThread = Thread.currentThread();
        return currThread == ServerLifecycleHooks.getCurrentServer().serverThread; // Paper - Fix issues with detecting main thread properly, the only time Watchdog will be used is during a crash shutdown which is a "try our best" scenario
        // Tuinity End
    }
}
