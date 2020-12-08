package fr.modcraftmc.modcraftforge.entity.tracker;

public class EntityTrackerManager {

    private static CheckTask checkTask = new CheckTask();
    private static UntrackerTask untrackerTask = new UntrackerTask();



    public static void runCheck() {

        checkTask.run();

    }

    public static void runUntracker() {

        untrackerTask.run();

    };
}
