package io.papermc.paper.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaperJvmChecker {

    private static int getJvmVersion() {
        String javaVersion = System.getProperty("java.version");
        final Matcher matcher = Pattern.compile("(?:1\\.)?(\\d+)").matcher(javaVersion);
        if (!matcher.find()) {
            LogManager.getLogger().warn("Failed to determine Java version; Could not parse: {}", javaVersion);
            return -1;
        }

        final String version = matcher.group(1);
        try {
            return Integer.parseInt(version);
        } catch (final NumberFormatException e) {
            LogManager.getLogger().warn("Failed to determine Java version; Could not parse {} from {}", version, javaVersion, e);
            return -1;
        }
    }

    public static void checkJvm() {
        if (getJvmVersion() < 11) {
            final Logger logger = LogManager.getLogger();
            logger.warn("************************************************************");
            logger.warn("* WARNING - YOU ARE RUNNING AN OUTDATED VERSION OF JAVA.");
            logger.warn("* MODCRAFTFORGE WILL STOP BEING COMPATIBLE WITH THIS VERSION OF");
            logger.warn("* JAVA WHEN MINECRAFT 1.17 IS RELEASED.");
            logger.warn("*");
            logger.warn("* Please update the version of Java you use to run Paper");
            logger.warn("* to at least Java 11. When Paper for Minecraft 1.17 is");
            logger.warn("* released support for versions of Java before 11 will");
            logger.warn("* be dropped.");
            logger.warn("*");
            logger.warn("* Current Java version: {}", System.getProperty("java.version"));
            logger.warn("*");
            logger.warn("* Check this forum post for more information: ");
            logger.warn("*   https://papermc.io/java11");
            logger.warn("************************************************************");
        }
    }
}
