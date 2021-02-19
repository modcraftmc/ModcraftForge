package common.mixins.krypton.flushconsolidation;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.steinborn.krypton.network.util.AutoFlushUtil.setAutoFlush;


@Mixin(TrackedEntity.class)
public class EntityTrackerMixin {

    @Inject(at = @At("HEAD"), method = "track")
    public void startTracking$disableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, false);
    }

    @Inject(at = @At("RETURN"), method = "track")
    public void startTracking$reenableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, true);
    }
}
