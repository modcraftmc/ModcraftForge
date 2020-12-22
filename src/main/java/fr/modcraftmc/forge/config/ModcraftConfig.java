package fr.modcraftmc.forge.config;

public class ModcraftConfig {

    private static ModcraftConfig instance;

    private static int arrowDespawnRate;
    private static int animalActivationRange = 32;
    private static int monsterActivationRange = 32;
    private static int miscActivationRange = 16;

    private static int playerTrackingRange = 48;
    private static int animalTrackingRange = 48;
    private static int monsterTrackingRange = 48;
    private static int miscTrackingRange = 32;
    private static int otherTrackingRange = 64;

    public ModcraftConfig() {
        arrowDespawnRate = 200;
    }

    public static int getAnimalActivationRange() {
        return animalActivationRange;
    }

    public static int getMonsterActivationRange() {
        return monsterActivationRange;
    }

    public static int getMiscActivationRange() {
        return miscActivationRange;
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

    public static int getArrowDespawnRate() {
        return arrowDespawnRate;
    }

    public static ModcraftConfig getInstance() {
        return instance == null ? instance = new ModcraftConfig() : instance;
    }
}
