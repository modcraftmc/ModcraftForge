package fr.modcraftmc.forge.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;

public class ModcraftCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("tpsbar")
                .requires((listener) -> {
                    return listener.hasPermissionLevel(2);
                })
                .executes((context) -> {
                    return execute(context.getSource().asPlayer());
                })
        );
    }

    private static int execute(ServerPlayerEntity player) {
        if (player != null) {
            TPSBarTask.togglePlayer(player);
            return 1;
        }
        return 0;
    }
}
