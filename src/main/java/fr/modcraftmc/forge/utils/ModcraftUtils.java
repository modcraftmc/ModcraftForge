package fr.modcraftmc.forge.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ModcraftUtils {

    public static boolean isPrimaryThread() {
        // Tuinity start
        final Thread currThread = Thread.currentThread();
        return currThread == ServerLifecycleHooks.getCurrentServer().serverThread; // Paper - Fix issues with detecting main thread properly, the only time Watchdog will be used is during a crash shutdown which is a "try our best" scenario
        // Tuinity End
    }

    /**
     * Gets the distance sqaured between 2 block positions
     * @param pos1
     * @param pos2
     * @return
     */
    public static double distanceSq(BlockPos pos1, BlockPos pos2) {
        return distanceSq(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    /**
     * Gets the distance squared between 2 positions
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return
     */
    public static double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2);
    }
}
