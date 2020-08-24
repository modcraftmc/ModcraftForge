package net.minecraftforge.modcraftforge.common.theading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModcraftThreadBox {

    public static final ExecutorService LOGIN_THREAD = Executors.newFixedThreadPool(2, new NamedThreadFactory("Player login thread"));

    public static final ExecutorService CHAT_THREAD = Executors.newSingleThreadExecutor(new NamedThreadFactory("Chat thread"));

    //public static final ExecutorService WORLDS_THREAD = Executors.newFixedThreadPool(4, new NamedThreadFactory("Worlds ticking thread"));


    public static class AssignableThread extends Thread {
        public AssignableThread(Runnable run) {
            super(run);
        }
        public AssignableThread() {
            super();
        }
    }
}
