package net.gegy1000.tictacs.chunk.upgrade;

import com.google.common.collect.Iterators;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;

import java.util.Iterator;

final class ChunkUpgradeEntries implements Iterable<ChunkEntry> {
    final ChunkUpgradeKernel kernel;
    final ChunkEntry[] entries;

    ChunkUpgradeEntries(ChunkUpgradeKernel kernel) {
        this.kernel = kernel;
        this.entries = kernel.create(ChunkEntry[]::new);
    }

    ChunkEntry getEntry(int x, int z) {
        return this.entries[this.kernel.index(x, z)];
    }

    @Override
    public Iterator<ChunkEntry> iterator() {
        return Iterators.forArray(this.entries);
    }
}
