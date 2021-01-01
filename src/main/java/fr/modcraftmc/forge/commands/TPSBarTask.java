package fr.modcraftmc.forge.commands;

import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.BossBarCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.CustomServerBossInfo;
import net.minecraft.server.CustomServerBossInfoManager;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class TPSBarTask implements ITickableTileEntity {

    private final ResourceLocation key;
    private CustomServerBossInfo bar;
    private static TPSBarTask instance;


    public TPSBarTask() {
        instance = this;
        CustomServerBossInfoManager bossbar = ServerLifecycleHooks.getCurrentServer().getCustomBossEvents();

        this.key = new ResourceLocation("modcraftforge", "tpsbar");

        CustomServerBossInfo bar = bossbar.get(key);

        if (bar == null) {
            bar = bossbar.add(key, new StringTextComponent("TPS: 20.0"));
            bar.setColor(BossInfo.Color.RED);
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

        double tps = ServerLifecycleHooks.getCurrentServer().getTPS()[0];
        if (tps > 20.0D) {
            tps = 20.0D;
        } else if (tps < 0.0D) {
            tps = 0.0D;
        }

        bar.setVisible(true);
        bar.setValue((int) Math.max(Math.min(tps / 20.0D, 1.0D), 0.0D));

        String tpsColor;
        if (tps >= 18) {
            tpsColor = "§2";
            bar.setColor(BossInfo.Color.GREEN);
        } else if (tps >= 15) {
            tpsColor = "§e";
            bar.setColor(BossInfo.Color.YELLOW);
        } else {
            tpsColor = "§4";
            bar.setColor(BossInfo.Color.RED);
        }

        double mspt = ServerLifecycleHooks.getCurrentServer().getTickTime();
        String msptColor;
        if (mspt < 40) {
            msptColor = "§2";
        } else if (mspt < 50) {
            msptColor = "§e";
        } else {
            msptColor = "§4";
        }

        bar.setName(new StringTextComponent("§eTPS§3: " + tpsColor + String.format("%.2f", tps) + "§eMSPT§3: " + msptColor + String.format("%.3f", mspt)));
    }
}
