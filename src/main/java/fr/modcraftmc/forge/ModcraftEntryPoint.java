package fr.modcraftmc.forge;

import com.velocitypowered.natives.util.Natives;
import fr.modcraftforge.forge.ModcraftForge;
import io.netty.util.ResourceLeakDetector;

public class ModcraftEntryPoint {

    public static boolean bungee = true;

    public static void load() {

        ModcraftForge.LOGGER.info("Krypton is now accelerating your Minecraft's networking stack \uD83D\uDE80");

        // By default, Netty allocates 16MiB arenas for the PooledByteBufAllocator. This is too much
        // memory for Minecraft, which imposes a maximum packet size of 2MiB! We'll use 4MiB as a more
        // sane default.
        //
        // Note: io.netty.allocator.pageSize << io.netty.allocator.maxOrder is the formula used to
        // compute the chunk size. We lower maxOrder from its default of 11 to 9. (We also use a null
        // check, so that the user is free to choose another setting if need be.)
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }

        // Disable the resource leak detector by default as it reduces performance. Allow the user to
        // override this if desired.
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }

        ModcraftForge.LOGGER.info("Compression will use " + Natives.compress.getLoadedVariant() + ", encryption will use " + Natives.cipher.getLoadedVariant());

    }
}
