package fr.modcraftmc.forge.config;

public class PaperConfig {

    private static PaperConfig instance;

    public int animalActivationRange = 32;
    public int monsterActivationRange = 32;
    public int raiderActivationRange = 48;
    public int miscActivationRange = 16;

    public int playerTrackingRange = 48;
    public int animalTrackingRange = 48;
    public int monsterTrackingRange = 48;
    public int miscTrackingRange = 32;
    public int otherTrackingRange = 64;

    // Paper start
    public int flyingMonsterActivationRange = 32;
    public int waterActivationRange = 16;
    public int villagerActivationRange = 32;
    public int wakeUpInactiveAnimals = 4;
    public int wakeUpInactiveAnimalsEvery = 60*20;
    public int wakeUpInactiveAnimalsFor = 5*20;
    public int wakeUpInactiveMonsters = 8;
    public int wakeUpInactiveMonstersEvery = 20*20;
    public int wakeUpInactiveMonstersFor = 5*20;
    public int wakeUpInactiveVillagers = 4;
    public int wakeUpInactiveVillagersEvery = 30*20;
    public int wakeUpInactiveVillagersFor = 5*20;
    public int wakeUpInactiveFlying = 8;
    public int wakeUpInactiveFlyingEvery = 10*20;
    public int wakeUpInactiveFlyingFor = 5*20;
    public int villagersWorkImmunityAfter = 5*20;
    public int villagersWorkImmunityFor = 20;
    public boolean villagersActiveForPanic = true;
    // Paper end

    public static PaperConfig GetConfigs(){
        return instance == null ? instance = new PaperConfig() : instance;
    }

}
