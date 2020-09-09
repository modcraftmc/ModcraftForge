package net.minecraftforge.server.command.ModcraftForge;

import com.mojang.brigadier.builder.ArgumentBuilder;
import fr.modcraftmc.modcraftforge.ModcraftConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;

public class GithubCommand {

    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal("github")
                .requires(cs->cs.hasPermissionLevel(4))
                .executes((ctx)-> {
                            ModcraftConfig.load();
                            ctx.getSource().sendFeedback(new StringTextComponent("github of the project: https://github.com/modcraftmc/ModcraftForge").applyTextStyle((text)-> {
                                text.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/modcraftmc/ModcraftForge"));
                            }), false);
                            return 1;
                        }
                );
    }
}
