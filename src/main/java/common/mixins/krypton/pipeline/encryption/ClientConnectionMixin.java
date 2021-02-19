package common.mixins.krypton.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.network.ClientConnectionEncryptionExtension;
import me.steinborn.krypton.network.pipeline.MinecraftCipherDecoder;
import me.steinborn.krypton.network.pipeline.MinecraftCipherEncoder;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(NetworkManager.class)
public class ClientConnectionMixin implements ClientConnectionEncryptionExtension {
    @Shadow private boolean isEncrypted;
    @Shadow private Channel channel;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        VelocityCipher decryption = Natives.cipher.get().forDecryption(key);
        VelocityCipher encryption = Natives.cipher.get().forEncryption(key);

        this.isEncrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new MinecraftCipherDecoder(decryption));
        this.channel.pipeline().addBefore("prepender", "encrypt", new MinecraftCipherEncoder(encryption));
    }
}
