package common.mixins.tictacs.packet_queue;

import net.gegy1000.tictacs.QueuingConnection;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Redirect(
            method = "sendChunkLoad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
            )
    )
    private void sendInitialChunkPacket(ServerPlayNetHandler network, IPacket<?> packet) {
        QueuingConnection.enqueueSend(network, packet);
    }

    @Redirect(
            method = "sendChunkUnload",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
            )
    )
    private void sendUnloadChunkPacket(ServerPlayNetHandler network, IPacket<?> packet) {
        QueuingConnection.enqueueSend(network, packet);
    }
}
