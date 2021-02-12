package common.mixins.tictacs.packet_queue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.gegy1000.tictacs.QueuingConnection;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

@Mixin(NetworkManager.class)
public abstract class ClientConnectionMixin implements QueuingConnection {
    @Shadow
    @Final
    private Queue<NetworkManager.QueuedPacket> outboundPacketsQueue;

    @Shadow
    private Channel channel;

    @Shadow
    private int field_211395_r;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract void setConnectionState(ProtocolType state);

    @Override
    public void enqueueSend(IPacket<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        this.outboundPacketsQueue.add(new NetworkManager.QueuedPacket(packet, callback));
    }

    /**
     * @reason send multiple queued packets by only scheduling to the event loop once
     * @author gegy1000
     */
    @Overwrite
    public void flushOutboundQueue() {
        if (this.channel == null || !this.channel.isOpen()) {
            return;
        }

        List<NetworkManager.QueuedPacket> queue = this.drainQueue();
        if (queue.isEmpty()) {
            return;
        }

        ProtocolType currentState = this.channel.attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get();

        NetworkManager.QueuedPacket lastPacket = queue.get(queue.size() - 1);
        ProtocolType lastState = ProtocolType.getFromPacket(lastPacket.packet);

        this.field_211395_r += queue.size();

        if (lastState != currentState) {
            LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            this.sendQueue(queue, currentState, lastState);
        } else {
            this.channel.eventLoop().execute(() -> {
                this.sendQueue(queue, currentState, lastState);
            });
        }
    }

    @Unique
    private List<NetworkManager.QueuedPacket> drainQueue() {
        if (this.outboundPacketsQueue.isEmpty()) {
            return Collections.emptyList();
        }

        List<NetworkManager.QueuedPacket> buffer = new ArrayList<>(this.outboundPacketsQueue.size());

        NetworkManager.QueuedPacket queued;
        while ((queued = this.outboundPacketsQueue.poll()) != null) {
            buffer.add(queued);
        }

        return buffer;
    }

    @Unique
    private void sendQueue(List<NetworkManager.QueuedPacket> queue, ProtocolType currentState, ProtocolType lastState) {
        if (lastState != currentState) {
            this.setConnectionState(lastState);
        }

        for (NetworkManager.QueuedPacket packet : queue) {
            ChannelFuture future = this.channel.write(packet.packet);
            if (packet.field_201049_b != null) {
                future.addListener(packet.field_201049_b);
            }
            future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        this.channel.flush();
    }
}
