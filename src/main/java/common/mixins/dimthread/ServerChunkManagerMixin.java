package common.mixins.dimthread;

import dimthread.DimThread;
import dimthread.IMutableMainThread;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerChunkProvider.class, priority = 1001)
public abstract class ServerChunkManagerMixin extends AbstractChunkProvider implements IMutableMainThread {

	@Shadow @Final @Mutable private Thread mainThread;
	@Shadow @Final public ChunkManager chunkManager;
	@Shadow @Final private ServerWorld world;

	@Override
	public Thread getMainThread() {
		return this.mainThread;
	}

	@Override
	public void setMainThread(Thread thread) {
		this.mainThread = thread;
	}

	@Inject(method = "getLoadedChunkCount", at = @At("HEAD"), cancellable = true)
	private void getTotalChunksLoadedCount(CallbackInfoReturnable<Integer> ci) {
		if(true) {
			int count = this.chunkManager.getLoadedChunkCount();
			if(count < 441)ci.setReturnValue(441);
		}
	}

	@Redirect(method = "getChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
	public Thread currentThread(int x, int z, ChunkStatus leastStatus, boolean create) {
		Thread thread = Thread.currentThread();

		if(DimThread.MANAGER.isActive(this.world.getServer()) && DimThread.owns(thread)) {
			return this.mainThread;
		}

		return thread;
	}

}
