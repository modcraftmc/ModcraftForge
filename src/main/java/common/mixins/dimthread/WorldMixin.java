package common.mixins.dimthread;

import dimthread.IMutableMainThread;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class WorldMixin implements IMutableMainThread {

	@Mutable @Shadow @Final private Thread mainThread;

	@Override
	public Thread getMainThread() {
		return this.mainThread;
	}

	@Override
	public void setMainThread(Thread thread) {
		this.mainThread = thread;
	}

}
