package common.mixins.tictacs.packet_queue;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.gegy1000.tictacs.QueuingConnection;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(ServerPlayNetHandler.class)
public class ServerPlayNetworkHandlerMixin implements QueuingConnection {
    @Shadow
    @Final
    public NetworkManager netManager;

    @Override
    public void enqueueSend(IPacket<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        ((QueuingConnection) this.netManager).enqueueSend(packet, callback);
    }
}
