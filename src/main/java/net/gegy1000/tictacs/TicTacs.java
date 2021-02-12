package net.gegy1000.tictacs;

import com.google.common.reflect.Reflection;
import net.gegy1000.tictacs.chunk.upgrade.ChunkUpgradeFuture;
import net.gegy1000.tictacs.config.TicTacsConfig;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TicTacs  {
    public static final String ID = "tic_tacs";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final ResourceLocation DEBUG_CHUNK_TICKETS =  new ResourceLocation(ID, "debug_chunk_tickets");

    public static final boolean DEBUG = true;

    static {
        TicTacsConfig.get();

        // due to a classloader bug in multithreaded environments, we need to load the class before multiple threads
        // try to load it concurrently
        Reflection.initialize(ChunkUpgradeFuture.class);
    }
}
