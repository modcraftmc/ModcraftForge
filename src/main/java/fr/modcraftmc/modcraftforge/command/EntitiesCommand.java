package fr.modcraftmc.modcraftforge.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.modcraftmc.modcraftforge.configuration.EntitiesConfiguration;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;

public class EntitiesCommand {

    static ArgumentBuilder<CommandSource, ?> register()
    {
        /*
        return Commands.literal("skipentities")
                .requires(cs->cs.hasPermissionLevel(4))
                .then(Commands.argument("value", StringArgumentType.word()))
                .executes((ctx)-> {
                            boolean value = Boolean.getBoolean(StringArgumentType.getString(ctx, "value"));
                            EntitiesConfiguration.skipEntities = value;
                            ctx.getSource().sendFeedback(new StringTextComponent("Value skipentities set to" + value), false);
                            return 1;
                        }
                );

         */

        return Commands.literal("entities")
                .requires(cs->cs.hasPermissionLevel(4))
                .then(Commands.literal("skipentities")
                        .then(Commands.argument("isSkipping", BoolArgumentType.bool())
                        .executes(EntitiesCommand::setEntitiesSkip)))
                .then(Commands.literal("tickinterval")
                        .then(Commands.argument("tickInterval", IntegerArgumentType.integer(0, 10))
                        .executes(EntitiesCommand::setSkipInterval)));


    }

    private static int setSkipInterval(CommandContext<CommandSource> ctx) {

        EntitiesConfiguration.setTickinterval(IntegerArgumentType.getInteger(ctx, "tickInterval"));
        ctx.getSource().sendFeedback(new StringTextComponent("Value tickInvetval is set to " + IntegerArgumentType.getInteger(ctx, "tickInterval")), false);

        return 1;
    }

    private static int setEntitiesSkip(CommandContext<CommandSource> ctx) {

        EntitiesConfiguration.setSkipEntities(BoolArgumentType.getBool(ctx, "isSkipping"));
        ctx.getSource().sendFeedback(new StringTextComponent("Value SkipEntities is set to " + BoolArgumentType.getBool(ctx, "isSkipping")), false);

        return 1;
    }
}
