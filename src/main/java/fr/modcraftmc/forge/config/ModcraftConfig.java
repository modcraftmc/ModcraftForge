package fr.modcraftmc.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ModcraftConfig {

    public static class Server {
        public final ForgeConfigSpec.ConfigValue<Integer> playerAutoSaveRate;
        public final ForgeConfigSpec.ConfigValue<Integer> autoSavePeriod;

        public final ForgeConfigSpec.BooleanValue fullBoundingBoxLadders;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Server configuration settings")
                    .push("server");

            builder.push("settings");

            playerAutoSaveRate = builder
                    .comment("Set how often players should be saved. A value of -1 means it will pick a recommended value for you.")
                    .define("playerAutoSaveRate", -1);

            autoSavePeriod = builder
                    .comment("Instructs this world to use a specific value for auto-save instead of Minecraft’s global value.")
                    .define("autoSavePeriod", -1);

            fullBoundingBoxLadders = builder
                    .comment("Set this to true to check the entire entity's collision bounding box for ladders instead of just the block they are in. Causes noticeable differences in mechanics so default is vanilla behavior. Default: false.")
                    .translation("forge.configgui.fullBoundingBoxLadders")
                    .worldRestart()
                    .define("fullBoundingBoxLadders", false);

            builder.pop();
            builder.pop();
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final ModcraftConfig.Server SERVER;
    static {
        final Pair<ModcraftConfig.Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ModcraftConfig.Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}
