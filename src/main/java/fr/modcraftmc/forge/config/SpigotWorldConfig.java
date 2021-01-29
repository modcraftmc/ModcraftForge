package fr.modcraftmc.forge.config;

import fr.modcraftforge.forge.ModcraftForge;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.util.List;

public class SpigotWorldConfig {

    private final String worldName;
    private final YamlConfiguration config;
    private boolean verbose;

    public SpigotWorldConfig(String worldName)
    {
        this.worldName = worldName;
        this.config = SpigotConfig.config;
        init();
    }

    public void init()
    {
        this.verbose = getBoolean( "verbose", false ); // Paper

        ModcraftForge.LOGGER.info( "-------- World Settings For [" + worldName + "] --------" );
        SpigotConfig.readConfig( SpigotWorldConfig.class, this );
    }

    private void set(String path, Object val)
    {
        config.set( "world-settings.default." + path, val );
    }

    public boolean getBoolean(String path, boolean def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getBoolean( "world-settings." + worldName + "." + path, config.getBoolean( "world-settings.default." + path ) );
    }

    public double getDouble(String path, double def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getDouble( "world-settings." + worldName + "." + path, config.getDouble( "world-settings.default." + path ) );
    }

    public int getInt(String path) // Paper - private -> public
    {
        return config.getInt( "world-settings." + worldName + "." + path );
    }

    public int getInt(String path, int def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getInt( "world-settings." + worldName + "." + path, config.getInt( "world-settings.default." + path ) );
    }

    public <T> List getList(String path, T def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return (List<T>) config.getList( "world-settings." + worldName + "." + path, config.getList( "world-settings.default." + path ) );
    }

    public String getString(String path, String def) // Paper - private -> public
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getString( "world-settings." + worldName + "." + path, config.getString( "world-settings.default." + path ) );
    }

    private Object get(String path, Object def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.get( "world-settings." + worldName + "." + path, config.get( "world-settings.default." + path ) );
    }

    public int itemDespawnRate;
    private void itemDespawnRate()
    {
        itemDespawnRate = getInt( "item-despawn-rate", 6000 );
        ModcraftForge.LOGGER.info( "Item Despawn Rate: " + itemDespawnRate );
    }

    public int animalActivationRange = 32;
    public int monsterActivationRange = 32;
    public int raiderActivationRange = 48;
    public int miscActivationRange = 16;
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
    public boolean tickInactiveVillagers = true;

    private void activationRange()
    {
        boolean hasAnimalsConfig = config.getInt("entity-activation-range.animals", animalActivationRange) != animalActivationRange; // Paper
        animalActivationRange = getInt( "entity-activation-range.animals", animalActivationRange );
        monsterActivationRange = getInt( "entity-activation-range.monsters", monsterActivationRange );
        raiderActivationRange = getInt( "entity-activation-range.raiders", raiderActivationRange );
        miscActivationRange = getInt( "entity-activation-range.misc", miscActivationRange );
        // Paper start
        waterActivationRange = getInt( "entity-activation-range.water", waterActivationRange );
        villagerActivationRange = getInt( "entity-activation-range.villagers", hasAnimalsConfig ? animalActivationRange : villagerActivationRange );
        flyingMonsterActivationRange = getInt( "entity-activation-range.flying-monsters", flyingMonsterActivationRange );

        wakeUpInactiveAnimals = getInt("entity-activation-range.wake-up-inactive.animals-max-per-tick", wakeUpInactiveAnimals);
        wakeUpInactiveAnimalsEvery = getInt("entity-activation-range.wake-up-inactive.animals-every", wakeUpInactiveAnimalsEvery);
        wakeUpInactiveAnimalsFor = getInt("entity-activation-range.wake-up-inactive.animals-for", wakeUpInactiveAnimalsFor);

        wakeUpInactiveMonsters = getInt("entity-activation-range.wake-up-inactive.monsters-max-per-tick", wakeUpInactiveMonsters);
        wakeUpInactiveMonstersEvery = getInt("entity-activation-range.wake-up-inactive.monsters-every", wakeUpInactiveMonstersEvery);
        wakeUpInactiveMonstersFor = getInt("entity-activation-range.wake-up-inactive.monsters-for", wakeUpInactiveMonstersFor);

        wakeUpInactiveVillagers = getInt("entity-activation-range.wake-up-inactive.villagers-max-per-tick", wakeUpInactiveVillagers);
        wakeUpInactiveVillagersEvery = getInt("entity-activation-range.wake-up-inactive.villagers-every", wakeUpInactiveVillagersEvery);
        wakeUpInactiveVillagersFor = getInt("entity-activation-range.wake-up-inactive.villagers-for", wakeUpInactiveVillagersFor);

        wakeUpInactiveFlying = getInt("entity-activation-range.wake-up-inactive.flying-monsters-max-per-tick", wakeUpInactiveFlying);
        wakeUpInactiveFlyingEvery = getInt("entity-activation-range.wake-up-inactive.flying-monsters-every", wakeUpInactiveFlyingEvery);
        wakeUpInactiveFlyingFor = getInt("entity-activation-range.wake-up-inactive.flying-monsters-for", wakeUpInactiveFlyingFor);

        villagersWorkImmunityAfter = getInt( "entity-activation-range.villagers-work-immunity-after", villagersWorkImmunityAfter );
        villagersWorkImmunityFor = getInt( "entity-activation-range.villagers-work-immunity-for", villagersWorkImmunityFor );
        villagersActiveForPanic = getBoolean( "entity-activation-range.villagers-active-for-panic", villagersActiveForPanic );
        // Paper end
        tickInactiveVillagers = getBoolean( "entity-activation-range.tick-inactive-villagers", tickInactiveVillagers );

        ModcraftForge.LOGGER.info( "Entity Activation Range: An " + animalActivationRange + " / Mo " + monsterActivationRange + " / Ra " + raiderActivationRange + " / Mi " + miscActivationRange + " / Tiv " + tickInactiveVillagers );
    }

    public int playerTrackingRange = 48;
    public int animalTrackingRange = 48;
    public int monsterTrackingRange = 48;
    public int miscTrackingRange = 32;
    public int otherTrackingRange = 64;
    private void trackingRange()
    {
        playerTrackingRange = getInt( "entity-tracking-range.players", playerTrackingRange );
        animalTrackingRange = getInt( "entity-tracking-range.animals", animalTrackingRange );
        monsterTrackingRange = getInt( "entity-tracking-range.monsters", monsterTrackingRange );
        miscTrackingRange = getInt( "entity-tracking-range.misc", miscTrackingRange );
        otherTrackingRange = getInt( "entity-tracking-range.other", otherTrackingRange );
        ModcraftForge.LOGGER.info( "Entity Tracking Range: Pl " + playerTrackingRange + " / An " + animalTrackingRange + " / Mo " + monsterTrackingRange + " / Mi " + miscTrackingRange + " / Other " + otherTrackingRange );
    }



}
