package common.mixins.krypton.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.LegacyPingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link LegacyPingHandler} to fix a security issue.
 */
@Mixin(LegacyPingHandler.class)
public abstract class LegacyQueryHandlerMixin {
    @Inject(method = "channelRead", at = @At(value = "HEAD"), cancellable = true)
    public void channelRead(ChannelHandlerContext ctx, Object msg, CallbackInfo ci) throws Exception {
        if (!ctx.channel().isActive()) {
            ((ByteBuf) msg).clear();
            ci.cancel();
        }
    }
}
