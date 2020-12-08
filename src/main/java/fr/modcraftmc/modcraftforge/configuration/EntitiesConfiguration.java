package fr.modcraftmc.modcraftforge.configuration;

public class EntitiesConfiguration {

    private static boolean skipEntities = true;
    private static int tickinterval = 1;

    public static boolean isSkipEntities() {
        return skipEntities;
    }

    public static int getTickinterval() {
        return tickinterval;
    }

    public static void setSkipEntities(boolean skipEntities) {
        EntitiesConfiguration.skipEntities = skipEntities;
    }

    public static void setTickinterval(int tickinterval) {
        EntitiesConfiguration.tickinterval = tickinterval;
    }


    private static boolean useEntityChecker = true;

    public static boolean isUseEntityChecker() {
        return useEntityChecker;
    }

    public static void setUseEntityChecker(boolean useEntityChecker) {
        EntitiesConfiguration.useEntityChecker = useEntityChecker;
    }
}
