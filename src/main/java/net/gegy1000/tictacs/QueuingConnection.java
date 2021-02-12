package net.gegy1000.tictacs;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;

public interface QueuingConnection {
    static void enqueueSend(ServerPlayNetHandler network, IPacket<?> packet) {
        ((QueuingConnection) network).enqueueSend(packet);
    }

    static void enqueueSend(ServerPlayNetHandler network, IPacket<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        ((QueuingConnection) network).enqueueSend(packet, callback);
    }

    default void enqueueSend(IPacket<?> packet) {
        this.enqueueSend(packet, null);
    }

    void enqueueSend(IPacket<?> packet, GenericFutureListener<? extends Future<? super Void>> callback);
}
