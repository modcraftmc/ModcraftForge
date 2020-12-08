package fr.modcraftmc.modcraftforge.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.modcraftmc.modcraftforge.configuration.EntitiesConfiguration;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class CheckerCommand {
    public static ArgumentBuilder<CommandSource,?> register() {

        return Commands.literal("checker")
                .requires(cs->cs.hasPermissionLevel(4))
                .then(Commands.argument("enabled", BoolArgumentType.bool()))
                .executes(CheckerCommand::setState);

    }

    private static int setState(CommandContext<CommandSource> ctx) {

        EntitiesConfiguration.setUseEntityChecker(BoolArgumentType.getBool(ctx, "enabled"));
        ctx.getSource().sendFeedback(new StringTextComponent("Value checker is set to " + BoolArgumentType.getBool(ctx, "enabled")), false);

        return 1;
    }
}
