package net.minecraftforge.server.command.modcraftforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;

public class ModcraftCommand {

    public ModcraftCommand(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("modcraftforge").executes(this::info)
                        .then(ReloadCommand.register())
                        .then(GithubCommand.register())

        );
    }

    public int info(CommandContext<CommandSource> source) {

        source.getSource().sendFeedback(new StringTextComponent("This server is running Forge, implementing ModcraftForge by modcraftmc developement team."), false);
        return 1;
    }
}
