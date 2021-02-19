package common.mixins.krypton.misc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Mixin(PacketBuffer.class)
public abstract class PacketByteBufMixin extends ByteBuf {

    @Shadow @Final private ByteBuf buf;

    @Shadow public abstract int writeCharSequence(CharSequence charSequence, Charset charset);

    /**
     * @author Andrew
     * @reason Use {@link ByteBuf#writeCharSequence(CharSequence, Charset)} instead for improved performance along with
     *         computing the byte size ahead of time with {@link ByteBufUtil#utf8Bytes(CharSequence)}
     */
    @Overwrite
    public PacketBuffer writeString(String string, int i) {
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        if (utf8Bytes > i) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + i + ")");
        } else {
            this.writeVarInt(utf8Bytes);
            this.writeCharSequence(string, StandardCharsets.UTF_8);
            return new PacketBuffer(buf);
        }
    }

    @Shadow public abstract PacketBuffer writeVarInt(int i);
}
