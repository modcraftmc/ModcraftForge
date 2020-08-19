package net.minecraftforge.server.command.ModcraftForge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

public class ModcraftCommand {

    public ModcraftCommand(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("modcraftforge")
                        .then(ReloadCommand.register())

        );
    }
}
