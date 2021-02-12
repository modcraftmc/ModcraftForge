package common.mixins.tictacs;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkManager.class)
public interface TacsAccessor {
    @Accessor("loadedPositions")
    LongSet getLoadedChunks();

    @Accessor("unloadableChunks")
    LongSet getQueuedUnloads();

    @Accessor("chunksToUnload")
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getUnloadingChunks();

    @Accessor("field_219263_q")
    ChunkTaskPriorityQueueSorter getChunkTaskPrioritySystem();
}
