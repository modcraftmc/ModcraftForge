package fr.modcraftmc.forge.commands;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.CustomServerBossInfo;
import net.minecraft.server.CustomServerBossInfoManager;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.text.DecimalFormat;

public class EntityBarTask implements ITickableTileEntity {

    private final ResourceLocation key;
    private CustomServerBossInfo bar;
    private static EntityBarTask instance;

    public static void onServerStartedEvent() {
        new EntityBarTask();
    }


    public EntityBarTask() {
        instance = this;
        CustomServerBossInfoManager bossbar = ServerLifecycleHooks.getCurrentServer().getCustomBossEvents();

        this.key = new ResourceLocation("modcraftforge", "entitybar");

        CustomServerBossInfo bar = bossbar.get(key);

        if (bar == null) {
            bar.setColor(BossInfo.Color.GREEN);
            bar.setOverlay(BossInfo.Overlay.NOTCHED_20);
            bar.setVisible(true);
        }
        this.bar = bar;
        ServerLifecycleHooks.getCurrentServer().registerTickable(this::tick);
    }

    public static void togglePlayer(ServerPlayerEntity player) {

        if (instance.bar.getPlayers().contains(player)) {
            instance.bar.removePlayer(player);
        } else {
            instance.bar.addPlayer(player);
            instance.tick();
        }

    }

    public static void removePlayer(ServerPlayerEntity player) {
        instance.bar.removePlayer(player);
    }

    public void tick() {
        if (bar.getPlayers().isEmpty()) {
            return;
        }

        bar.setVisible(true);
        ServerWorld serverWorld = ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD);
        int entityCount = (int) serverWorld.getEntities().count();


        bar.setName(new StringTextComponent(String.format("Entities count: %s | Entities ticktime %sms", entityCount, serverWorld.entitiesTickTime)));
    }
}
