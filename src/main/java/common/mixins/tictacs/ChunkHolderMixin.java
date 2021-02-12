package common.mixins.tictacs;

import net.gegy1000.tictacs.QueuingConnection;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Shadow
    @Final
    private static ChunkHolder.LocationType[] LOCATION_TYPES;

    /**
     * @reason we replace the future handling in {@link ChunkEntry}, and we don't want
     * vanilla's logic to mess with ours.
     * @author gegy1000
     */
    @Redirect(method = "processUpdates", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;complete(Ljava/lang/Object;)Z"))
    private <T> boolean complete(CompletableFuture<T> future, T result) {
        return true;
    }

    /**
     * @reason replace with chunk step logic
     * @author gegy1000
     */
    @Overwrite
    public static ChunkStatus getChunkStatusFromLevel(int level) {
        return ChunkEntry.getTargetStep(level).getMaximumStatus();
    }

    /**
     * @reason replace full level constant
     * @author gegy1000
     */
    @Overwrite
    public static ChunkHolder.LocationType getLocationTypeFromLevel(int distance) {
        return LOCATION_TYPES[MathHelper.clamp(ChunkLevelTracker.FULL_LEVEL - distance + 1, 0, LOCATION_TYPES.length - 1)];
    }
}
