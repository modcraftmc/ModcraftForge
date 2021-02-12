package net.gegy1000.tictacs.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TicTacsDebugRenderer implements DebugRenderer.IDebugRenderer {
	@Override
	public void render(MatrixStack matrices, IRenderTypeBuffer vertexConsumers, double cameraX, double cameraY, double cameraZ) {
		RenderSystem.pushMatrix();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.color4f(0.0F, 1.0F, 0.0F, 0.75F);
		RenderSystem.disableTexture();

		long systime = System.currentTimeMillis();

		TicTacsDebugLevelTracker tracker = TicTacsDebugLevelTracker.INSTANCE;

		LongIterator iterator = tracker.chunks().iterator();
		while (iterator.hasNext()) {
			long pos = iterator.nextLong();
			int chunkX = ChunkPos.getX(pos);
			int chunkZ = ChunkPos.getZ(pos);

			int level = tracker.getLevel(pos);
			long redTime = tracker.getRedTime(pos);

			double x = (chunkX << 4) + 8.5;
			double y = 128 + 1.2;
			double z = (chunkZ << 4) + 8.5;

			int color = systime <= redTime ? 0xff0000 : -1;

			DebugRenderer.renderText(String.valueOf(level), x, y, z, color, 0.25F, true, 0.0F, true);
		}

		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		RenderSystem.popMatrix();
	}
}
