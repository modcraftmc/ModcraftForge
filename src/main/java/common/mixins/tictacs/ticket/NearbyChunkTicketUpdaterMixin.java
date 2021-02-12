package common.mixins.tictacs.ticket;

import net.gegy1000.tictacs.chunk.ticket.TicketTracker;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

@Mixin(TicketManager.PlayerTicketTracker.class)
public class NearbyChunkTicketUpdaterMixin {
    @Shadow(aliases = "playerTicketTracker")
    private TicketManager ticketManager;

    /**
     * @author gegy1000
     * @see ChunkTicketManagerMixin
     */
    @Overwrite
    public void func_215504_a(long pos, int distance, boolean wasTracked, boolean isTracked) {
        if (wasTracked == isTracked) {
            return;
        }

        TicketTracker ticketTracker = (TicketTracker) this.ticketManager;
        if (isTracked) {
            ticketTracker.enqueueTicket(pos, distance);
        } else {
            ticketTracker.removeTicket(pos);
        }
    }

    @Redirect(
            method = "processAllUpdates",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/ChunkTaskPriorityQueueSorter;func_219066_a(Lnet/minecraft/util/math/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"
            )
    )
    private void updateLevel(ChunkTaskPriorityQueueSorter ctps, ChunkPos pos, IntSupplier getLevel, int targetLevel, IntConsumer setLevel) {
        TicketTracker ticketTracker = (TicketTracker) this.ticketManager;
        ticketTracker.moveTicket(pos.asLong(), getLevel.getAsInt(), targetLevel);
        setLevel.accept(targetLevel);
    }
}
