package common.mixins.tictacs.ticket;

import it.unimi.dsi.fastutil.longs.LongList;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.gegy1000.tictacs.chunk.ticket.PlayerTicketManager;
import net.gegy1000.tictacs.chunk.ticket.TicketTracker;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Comparator;
import java.util.Set;

@Mixin(TicketManager.class)
public abstract class ChunkTicketManagerMixin implements TicketTracker {
    private static final TicketType<ChunkPos> GENERATION_TICKET = TicketType.create("player_generation", Comparator.comparingLong(ChunkPos::asLong));

    @Shadow
    @Final
    public TicketManager.PlayerChunkTracker playerChunkTracker;
    @Shadow
    @Final
    public TicketManager.PlayerTicketTracker playerTicketTracker;
    @Shadow
    @Final
    public TicketManager.ChunkTicketTracker ticketTracker;
    @Shadow
    @Final
    public Set<ChunkHolder> chunkHolders;

    private PlayerTicketManager fullTickets;
    private PlayerTicketManager generationTickets;

    /**
     * @reason redirect player ticket logic to {@link PlayerTicketManager}
     * @author gegy1000
     */
    @Overwrite
    public boolean processUpdates(ChunkManager tacs) {
        this.initialize(tacs);

        LongList fullTickets = this.fullTickets.collectTickets();
        LongList generationTickets = this.generationTickets.collectTickets();

        this.playerChunkTracker.processAllUpdates();
        this.playerTicketTracker.processAllUpdates();

        int completedTasks = Integer.MAX_VALUE - this.ticketTracker.func_215493_a(Integer.MAX_VALUE);

        this.fullTickets.waitForChunks(fullTickets);
        this.generationTickets.waitForChunks(generationTickets);

        if (!this.chunkHolders.isEmpty()) {
            for (ChunkHolder holder : this.chunkHolders) {
                ChunkEntry entry = (ChunkEntry) holder;
                entry.onUpdateLevel(tacs);
            }
            this.chunkHolders.clear();
            return true;
        }

        return completedTasks != 0;
    }

    @Unique
    private void initialize(ChunkManager tacs) {
        if (this.fullTickets == null || this.generationTickets == null) {
            ChunkController controller = (ChunkController) tacs;
            this.fullTickets = new PlayerTicketManager(controller, ChunkStep.FULL, 2, TicketType.PLAYER, 2);
            this.generationTickets = new PlayerTicketManager(controller, ChunkStep.GENERATION, 0, GENERATION_TICKET, 5);
        }
    }

    @Override
    public void enqueueTicket(long pos, int distance) {
        this.fullTickets.enqueueTicket(pos, distance);
        this.generationTickets.enqueueTicket(pos, distance);
    }

    @Override
    public void removeTicket(long pos) {
        this.fullTickets.removeTicket(pos);
        this.generationTickets.removeTicket(pos);
    }

    @Override
    public void moveTicket(long pos, int fromDistance, int toDistance) {
        this.fullTickets.moveTicket(pos, fromDistance, toDistance);
        this.generationTickets.moveTicket(pos, fromDistance, toDistance);
    }
}
