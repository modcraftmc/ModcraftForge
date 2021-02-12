package common.mixins.tictacs.lighting;

import net.gegy1000.tictacs.VoidActor;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;

@Mixin(ChunkManager.class)
public abstract class ThreadedAnvilChunkStorageMixin implements ChunkController {
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/concurrent/DelegatedTaskExecutor;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/concurrent/DelegatedTaskExecutor;",
                    ordinal = 1
            )
    )
    private DelegatedTaskExecutor<Runnable> createLightingActor(Executor executor, String name) {
        return new VoidActor(name);
    }
}
