package fr.modcraftmc.forge.threading.threads;

import net.minecraft.util.Util;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerWorkerThread extends Thread {
    private static final AtomicInteger threadId = new AtomicInteger(1);
    public ServerWorkerThread(Runnable target, String poolName, int prorityModifier) {
        super(target, "Worker-" + poolName + threadId.getAndIncrement());
        setPriority(Thread.NORM_PRIORITY+prorityModifier);
        this.setDaemon(true);
        this.setUncaughtExceptionHandler(Util::printException);
    }
}
