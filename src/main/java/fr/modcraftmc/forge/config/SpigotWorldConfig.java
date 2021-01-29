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
