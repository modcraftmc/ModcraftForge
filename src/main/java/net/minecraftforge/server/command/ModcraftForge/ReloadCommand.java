package net.minecraftforge.server.command.ModcraftForge;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.modcraftforge.common.ModcraftConfig;

public class ReloadCommand {

    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal("reload")
                .requires(cs->cs.hasPermissionLevel(4))
                .executes((ctx)-> {
                    ModcraftConfig.load();
                    ctx.getSource().sendFeedback(new StringTextComponent("ModcraftForge config reloaded !"), true);
                    return 1;
                }
        );
    }
}
