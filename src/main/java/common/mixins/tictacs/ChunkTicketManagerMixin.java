package common.mixins.tictacs;

import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(TicketManager.class)
public class ChunkTicketManagerMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 33))
    private int getFullLevelForNearbyChunkTicketUpdater(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 33))
    private static int getFullLevelForNearbyPlayerTicketLevel(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }

    @ModifyConstant(method = "register(Lnet/minecraft/world/server/TicketType;Lnet/minecraft/util/math/ChunkPos;ILjava/lang/Object;)V", constant = @Constant(intValue = 33))
    private int getFullChunkLevelForAddTicket(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }

    @ModifyConstant(method = "release(Lnet/minecraft/world/server/TicketType;Lnet/minecraft/util/math/ChunkPos;ILjava/lang/Object;)V", constant = @Constant(intValue = 33))
    private int getFullChunkLevelForRemoveTicket(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }
}
