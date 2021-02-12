package common.mixins.tictacs.io;

import net.gegy1000.tictacs.AsyncChunkIo;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkLoader.class)
public class VersionedChunkStorageMixin implements AsyncChunkIo {
    @Shadow
    @Final
    private IOWorker field_227077_a_;

    @Override
    public CompletableFuture<CompoundNBT> getNbtAsync(ChunkPos pos) {
        return ((AsyncChunkIo) this.field_227077_a_).getNbtAsync(pos);
    }
}
