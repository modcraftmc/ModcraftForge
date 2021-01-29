package fr.modcraftmc.forge.config;

import com.google.common.base.Throwables;
import fr.modcraftforge.forge.ModcraftForge;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class SpigotConfig {

    private static File CONFIG_FILE = new File("spigot.yml");
    private static String HEADER = "Simple reproduction of Spigot configuration file, all options will not be available.\n";

    public static YamlConfiguration config;

    public static void init() {
        config = new YamlConfiguration();

        try {
            config.load(CONFIG_FILE);
        } catch (IOException ignored) {
        } catch (InvalidConfigurationException e) {
            ModcraftForge.LOGGER.warn("Could not load spigot.yml, please correct your syntax errors", e);
        }

        //config.options().header(HEADER);
        config.options().copyDefaults( true );

        readConfig(SpigotConfig.class, null);
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {

                    }
                }
            }
        }
        save();
    }

    public static void save() {
        // Paper end
        try
        {
            config.save( CONFIG_FILE );
        } catch ( IOException ex )
        {
        }
    }


    private static void set(String path, Object val)
    {
        config.set( path, val );
    }

    private static boolean getBoolean(String path, boolean def)
    {
        config.addDefault( path, def );
        return config.getBoolean( path, config.getBoolean( path ) );
    }

    private static int getInt(String path, int def)
    {
        config.addDefault( path, def );
        return config.getInt( path, config.getInt( path ) );
    }

    private static <T> List getList(String path, T def)
    {
        config.addDefault( path, def );
        return (List<T>) config.getList( path, config.getList( path ) );
    }

    private static String getString(String path, String def)
    {
        config.addDefault( path, def );
        return config.getString( path, config.getString( path ) );
    }

    private static double getDouble(String path, double def)
    {
        config.addDefault( path, def );
        return config.getDouble( path, config.getDouble( path ) );
    }

    private static void nettyThreads()
    {
        int count = getInt( "settings.netty-threads", 4 );
        System.setProperty( "io.netty.eventLoopThreads", Integer.toString( count ) );
        ModcraftForge.LOGGER.info("Using {} threads for Netty based IO", count );
    }
}
