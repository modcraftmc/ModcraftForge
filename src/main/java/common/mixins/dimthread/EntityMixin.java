package common.mixins.dimthread;

import dimthread.DimThread;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

	@Shadow public abstract Entity changeDimension(ServerWorld destination);

	/**
	 * Schedules moving entities between dimensions to the server thread. Once all the world finish ticking,
	 * {@code moveToWorld()} is processed in a safe manner avoiding concurrent modification exceptions.
	 *
	 * For example, the entity list is not thread-safe and modifying it from multiple threads will cause
	 * a crash. Additionally, loading chunks from another thread will cause a deadlock in the server chunk manager.
	 * */
	@Inject(method = "changeDimension(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/entity/Entity;", at = @At("HEAD"), cancellable = true, remap = false)
	public void moveToWorld(ServerWorld destination, ITeleporter teleporter, CallbackInfoReturnable<Entity> cir) {
		if(!DimThread.MANAGER.isActive(destination.getServer()))return;

		if(DimThread.owns(Thread.currentThread())) {
			destination.getServer().execute(() -> this.changeDimension(destination));
			cir.setReturnValue(null);
		}
	}

}
