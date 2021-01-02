package fr.modcraftmc.forge.config;

public class ModcraftConfig {

    private static int arrowDespawnRate;

    private static int animalActivationRange = 32;
    private static int monsterActivationRange = 32;
    private static int raiderActivationRange = 48;
    private static int miscActivationRange = 16;
    private static int waterActivationRange = 16; // Paper
    private static boolean tickInactiveVillagers = true;

    private static int viewDistance = 7;

    private static int playerTrackingRange = 48;
    private static int animalTrackingRange = 48;
    private static int monsterTrackingRange = 48;
    private static int miscTrackingRange = 32;
    private static int otherTrackingRange = 64;

    private static boolean warnArgumentTypes = false;
    private static boolean showTagCollectionError = false;
    private static boolean showRecipeManagerError = false;
    private static boolean showLootTableError = false;

    public ModcraftConfig() {
        arrowDespawnRate = 200;
    }


    public static int getAnimalActivationRange() {
        return animalActivationRange;
    }

    public static int getMonsterActivationRange() {
        return monsterActivationRange;
    }

    public static int getRaiderActivationRange() {
        return raiderActivationRange;
    }

    public static int getMiscActivationRange() {
        return miscActivationRange;
    }

    public static int getWaterActivationRange() {
        return waterActivationRange;
    }

    public static boolean isTickInactiveVillagers() {
        return tickInactiveVillagers;
    }

    public static int getViewDistance() {
        return viewDistance;
    }

    public static int getArrowDespawnRate() {
        return arrowDespawnRate;
    }

    public static int getPlayerTrackingRange() {
        return playerTrackingRange;
    }

    public static int getAnimalTrackingRange() {
        return animalTrackingRange;
    }

    public static int getMonsterTrackingRange() {
        return monsterTrackingRange;
    }

    public static int getMiscTrackingRange() {
        return miscTrackingRange;
    }

    public static int getOtherTrackingRange() {
        return otherTrackingRange;
    }

    public static boolean isWarnArgumentTypes() {
        return warnArgumentTypes;
    }

    public static boolean isShowTagCollectionError() {
        return showTagCollectionError;
    }

    public static boolean isShowRecipeManagerError() {
        return showRecipeManagerError;
    }

    public static boolean isShowLootTableError() {
        return showLootTableError;
    }
}
