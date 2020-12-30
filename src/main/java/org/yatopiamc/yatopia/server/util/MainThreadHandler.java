package org.yatopiamc.yatopia.server.util;

public class MainThreadHandler {

    private static WeakCollection<Thread> weakMainThreads = new WeakCollection<Thread>();

    public static void registerThread(Thread thread) {
        weakMainThreads.add(thread);
    }

    public static boolean isMainThread(Thread thread) {
        return weakMainThreads.contains(thread);
    }
}
