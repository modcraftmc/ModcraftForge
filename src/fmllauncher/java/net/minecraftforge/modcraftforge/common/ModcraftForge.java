package net.minecraftforge.modcraftforge.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

public class ModcraftForge {

    public static final Logger LOGGER = LogManager.getLogger("ModcraftForge");
    private static ModcraftForge instance;


    public ModcraftForge() {
        instance = this;


        LOGGER.info("");
        LOGGER.info("  __  __           _                 __ _   _____                    ");
        LOGGER.info(" |  \\/  | ___   __| | ___ _ __ __ _ / _| |_|  ___|__  _ __ __ _  ___ ");
        LOGGER.info(" | |\\/| |/ _ \\ / _` |/ __| '__/ _` | |_| __| |_ / _ \\| '__/ _` |/ _ \\");
        LOGGER.info(" | |  | | (_) | (_| | (__| | | (_| |  _| |_|  _| (_) | | | (_| |  __/");
        LOGGER.info(" |_|  |_|\\___/ \\__,_|\\___|_|  \\__,_|_|  \\__|_|  \\___/|_|  \\__, |\\___|");
        LOGGER.info("                                                          |___/      ");
        LOGGER.info("");
        LOGGER.info("            This server is running ModcraftForge, if you have compatibility issues,");
        LOGGER.info("            please test on Forge server before reporting.");
        LOGGER.info("");

    }

    public static ModcraftForge get() {
        return instance;
    }
}
