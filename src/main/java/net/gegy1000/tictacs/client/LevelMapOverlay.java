package net.gegy1000.tictacs.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.gegy1000.tictacs.TicTacs;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;

public final class LevelMapOverlay extends AbstractGui implements AutoCloseable {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final ResourceLocation TEXTURE_ID = new ResourceLocation(TicTacs.ID, "level_map");

    private DynamicTexture texture;
    private int textureWidth;
    private int textureHeight;

    private long lastTextureUpdate;

    public void render(MatrixStack transform) {
        ClientWorld clientWorld = CLIENT.world;
        if (clientWorld == null) {
            return;
        }

        IntegratedServer server = CLIENT.getIntegratedServer();
        if (server == null) {
            return;
        }

        long time = clientWorld.getGameTime();
        if (time - this.lastTextureUpdate > 20) {
            ServerWorld serverWorld = server.getWorld(clientWorld.getDimensionKey());
            ChunkManager tacs = serverWorld.getChunkProvider().chunkManager;
            ChunkController controller = (ChunkController) tacs;

            NativeImage image = LevelMapRenderer.render(CLIENT.player.getPositionVec(), controller);
            this.updateTexture(image);

            this.lastTextureUpdate = time;
        }

        CLIENT.getTextureManager().bindTexture(TEXTURE_ID);
        AbstractGui.blit(transform, 0, 0, 0.0F, 0.0F, this.textureWidth, this.textureHeight, this.textureWidth, this.textureHeight);
    }

    private void updateTexture(NativeImage image) {
        this.releaseTexture();

        this.texture = new DynamicTexture(image);
        this.textureWidth = image.getWidth();
        this.textureHeight = image.getHeight();

        CLIENT.getTextureManager().loadTexture(TEXTURE_ID, this.texture);
    }

    @Override
    public void close() {
        this.releaseTexture();
        CLIENT.getTextureManager().deleteTexture(TEXTURE_ID);
    }

    private void releaseTexture() {
        if (this.texture != null) {
            this.texture.close();
        }
    }
}
