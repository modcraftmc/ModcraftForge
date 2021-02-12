package net.gegy1000.tictacs.chunk;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.minecraft.util.math.ChunkPos;

public interface ChunkAccess {
    void putEntry(ChunkEntry entry);

    ChunkEntry removeEntry(long pos);

    ChunkEntry getEntry(long pos);

    default ChunkEntry getEntry(int chunkX, int chunkZ) {
        return this.getEntry(ChunkPos.asLong(chunkX, chunkZ));
    }

    default ChunkEntry getEntry(ChunkPos pos) {
        return this.getEntry(pos.asLong());
    }

    default ChunkEntry expectEntry(int chunkX, int chunkZ) {
        ChunkEntry entry = this.getEntry(chunkX, chunkZ);
        if (entry == null) {
            throw new IllegalStateException("expected entry at [" + chunkX + ", " + chunkZ + "]");
        }
        return entry;
    }

    ObjectCollection<ChunkEntry> getEntries();
}
