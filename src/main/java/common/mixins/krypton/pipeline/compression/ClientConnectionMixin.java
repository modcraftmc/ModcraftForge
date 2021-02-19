package common.mixins.krypton.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.network.compression.MinecraftCompressDecoder;
import me.steinborn.krypton.network.compression.MinecraftCompressEncoder;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class ClientConnectionMixin {
    @Shadow private Channel channel;

    @Inject(method = "setCompressionThreshold", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int compressionThreshold, CallbackInfo ci) {
        if (compressionThreshold >= 0) {
            VelocityCompressor compressor = Natives.compress.get().create(4);
            MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
            MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(compressionThreshold, compressor);

            channel.pipeline().addBefore("decoder", "decompress", decoder);
            channel.pipeline().addBefore("encoder", "compress", encoder);
        } else {
            this.channel.pipeline().remove("decompress");
            this.channel.pipeline().remove("compress");
        }

        ci.cancel();
    }
}
