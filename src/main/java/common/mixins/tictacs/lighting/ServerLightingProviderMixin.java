package common.mixins.tictacs.lighting;

import net.gegy1000.tictacs.lighting.LightingExecutor;
import net.gegy1000.tictacs.lighting.LightingExecutorHolder;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

@Mixin(ServerWorldLightManager.class)
public abstract class ServerLightingProviderMixin extends WorldLightManager implements LightingExecutorHolder {
    @Unique
    private final LightingExecutor lightingExecutor = new LightingExecutor(this);

    private ServerLightingProviderMixin(IChunkLightProvider chunks, boolean blockLight, boolean skyLight) {
        super(chunks, blockLight, skyLight);
    }

    /**
     * @reason wake up the executor on each tick for lighting updates that have been indirectly queued
     * @author gegy1000
     */
    @Overwrite
    public void func_215588_z_() {
        this.lightingExecutor.wake();
    }

    /**
     * @reason delegate to the lighting executor
     * @author gegy1000
     */
    @Overwrite
    private void func_215600_a(int x, int z, IntSupplier levelSupplier, ServerWorldLightManager.Phase stage, Runnable task) {
        this.lightingExecutor.enqueue(task, stage);
    }

    /**
     * @reason allow doLightUpdates to be called from the executor
     * @author gegy1000
     */
    @Override
    @Overwrite
    public int tick(int maxUpdateCount, boolean doSkylight, boolean skipEdgeLightPropagation) {
        return super.tick(maxUpdateCount, doSkylight, skipEdgeLightPropagation);
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        this.lightingExecutor.close();
    }

    @Override
    public LightingExecutor getLightingExecutor() {
        return this.lightingExecutor;
    }
}
