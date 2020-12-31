package fr.modcraftmc.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;


public class ModcraftForge {

    public static Logger LOGGER = LogManager.getLogger("ModcraftForge");

    private static String version = "0.0.8";
    public static long startTime;

    public static void setup() {
        startTime = System.currentTimeMillis();

        LOGGER.info("");
        LOGGER.info("  __  __           _                 __ _   _____                    ");
        LOGGER.info(" |  \\/  | ___   __| | ___ _ __ __ _ / _| |_|  ___|__  _ __ __ _  ___ ");
        LOGGER.info(" | |\\/| |/ _ \\ / _` |/ __| '__/ _` | |_| __| |_ / _ \\| '__/ _` |/ _ \\");
        LOGGER.info(" | |  | | (_) | (_| | (__| | | (_| |  _| |_|  _| (_) | | | (_| |  __/");
        LOGGER.info(" |_|  |_|\\___/ \\__,_|\\___|_|  \\__,_|_|  \\__|_|  \\___/|_|  \\__, |\\___|");
        LOGGER.info("                                                          |___/      ");
        LOGGER.info("");
        LOGGER.info("            /--------------------------------------------------------------------------\\");
        LOGGER.info("            | You are running ModcraftForge, if you have compatibility issues,          | ");
        LOGGER.info("            | please test on Forge server before reporting.                             |");
        LOGGER.info("            | https://github.com/modcraftmc/ModcraftForge/issues                        |");
        LOGGER.info("            \\---------------------------------------------------------------------------/");
        LOGGER.info("");
    }

    public static String getFormatedStartTime() {
        return  String.format("Loading ModcraftForge - %s time elasped", new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - startTime)));
    }

    public static String getFullVersionBrand() {
        return "ModcraftForge@" + version;
    }

    public static String getVersionBrand() {
        return version;
    }
}
