package common.mixins.tictacs.actor;

import net.gegy1000.tictacs.OwnThreadActor;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;

@Mixin(DelegatedTaskExecutor.class)
public class TaskExecutorMixin<T> {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void create(Executor executor, String name, CallbackInfoReturnable<DelegatedTaskExecutor<Runnable>> ci) {
        if (executor == Util.getServerExecutor()) {
            ci.setReturnValue(OwnThreadActor.create(name));
        }
    }
}
