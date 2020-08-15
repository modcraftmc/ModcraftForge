package net.minecraftforge.modcraftforge.common;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.nio.file.Paths;

import static net.minecraftforge.fml.loading.LogMarkers.CORE;

public class ModcraftConfig {

    private static ModcraftConfig INSTANCE = new ModcraftConfig();
    private static ConfigSpec configSpec = new ConfigSpec();
    static {

        configSpec.define("LogDataFixer", Boolean.TRUE);
        configSpec.define("LogDimsLoading", Boolean.FALSE);
    }

    private CommentedFileConfig configData;

    private void loadFrom(final Path configFile)
    {
        configData = CommentedFileConfig.builder(configFile).sync().
                defaultResource("/META-INF/defaultmodcraftforge.toml").
                autosave().autoreload().
                writingMode(WritingMode.REPLACE).
                build();
        configData.load();
        if (!configSpec.isCorrect(configData)) {
            ModcraftForge.LOGGER.warn(CORE, "Configuration file {} is not correct. Correcting", configFile);
            configSpec.correct(configData, (action, path, incorrectValue, correctedValue) ->
                    ModcraftForge.LOGGER.warn(CORE, "Incorrect key {} was corrected from {} to {}", path, incorrectValue, correctedValue));
        }
        configData.save();
    }

    public static void load()
    {
        final Path configFile = FMLPaths.MODCRAFTFORGECONFIG.get();
        INSTANCE.loadFrom(configFile);
        FMLPaths.getOrCreateGameRelativePath(Paths.get(FMLConfig.defaultConfigPath()), "default config directory");
    }

    public static boolean logDataFixer() {
        return INSTANCE.configData.<Boolean>getOptional("LogDataFixer").orElse(Boolean.TRUE);
    }

    public static boolean logDimension() {
        return INSTANCE.configData.<Boolean>getOptional("LogDimsLoading").orElse(Boolean.TRUE);
    }
}
