package common.mixins.krypton.flushconsolidation;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.steinborn.krypton.network.ConfigurableAutoFlush;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimizes ClientConnection by adding the ability to skip auto-flushing and using void promises where possible.
 */
@Mixin(NetworkManager.class)
public abstract class ClientConnectionMixin implements ConfigurableAutoFlush {
    @Shadow private Channel channel;
    private AtomicBoolean autoFlush;

    @Shadow public abstract void setConnectionState(ProtocolType state);

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddedFields(CallbackInfo ci) {
        this.autoFlush = new AtomicBoolean(true);
    }

    /**
     * Refactored sendImmediately method. This is a better fit for {@code @Overwrite} but we have to write it this way
     * because the fabric-networking-api-v1 also mixes into this...
     *
     * @author Andrew Steinborn
     */
    @Inject(locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true,
            method = "dispatchPacket",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/NetworkManager;field_211395_r:I", opcode = Opcodes.GETFIELD, shift = At.Shift.AFTER))
    private void sendImmediately$rewrite(IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo info, ProtocolType packetState, ProtocolType protocolState) {
        boolean newState = packetState != protocolState;

        if (this.channel.eventLoop().inEventLoop()) {
            if (newState) {
                this.setConnectionState(packetState);
            }
            doSendPacket(packet, callback);
        } else {
            // In newer versions of Netty, we can avoid wakeups using arbitrary tasks by implementing
            // AbstractEventExecutor.LazyTask. But we are targeting an older version of Netty and don't
            // have that luxury. So we'll be a bit clever. If we directly invoke Channel#write, it will
            // use a WriteTask and that doesn't wake up the event loop. This does have a few preconditions
            // (we can't be transitioning states, and for simplicity, no callbacks are supported) but if
            // met this minimizes wakeups when we don't need to immediately do a syscall.
            if (!newState && callback == null) {
                ChannelPromise voidPromise = this.channel.voidPromise();
                if (this.autoFlush.get()) {
                    this.channel.writeAndFlush(packet, voidPromise);
                } else {
                    this.channel.write(packet, voidPromise);
                }
            } else {
                // Fallback.
                if (newState) {
                    this.channel.config().setAutoRead(false);
                }

                this.channel.eventLoop().execute(() -> {
                    if (newState) {
                        this.setConnectionState(packetState);
                    }
                    doSendPacket(packet, callback);
                });
            }
        }

        info.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/NetworkManager;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(NetworkManager clientConnection) {
        return null;
    }

    private void doSendPacket(IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.write(packet);
            channelFuture.addListener(callback);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        if (this.autoFlush.get()) {
            this.channel.flush();
        }
    }

    @Override
    public void setShouldAutoFlush(boolean shouldAutoFlush) {
        boolean prev = this.autoFlush.getAndSet(shouldAutoFlush);
        if (!prev && shouldAutoFlush) {
            this.channel.flush();
        }
    }
}
