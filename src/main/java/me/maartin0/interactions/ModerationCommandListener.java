package me.maartin0.interactions;

import me.maartin0.Main;
import me.maartin0.Rift;
import me.maartin0.Searcher;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static me.maartin0.interactions.UtilCommandListener.handleRiftInteraction;

public class ModerationCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getCommandPath()) {
            case "info" -> {
                Rift rift = handleRiftInteraction(event);
                if (rift == null) return;
                event.getHook().sendMessage(rift.getInfo()).queue();
            } case "description" -> {
                Rift rift = handleRiftInteraction(event);
                if (rift == null) return;
                event.getChannel().asTextChannel().getManager().setTopic(rift.getInfo()).queue();
                event.getHook().sendMessage("Success!").queue();
            }
        }
    }
    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (event.getCommandPath().equalsIgnoreCase("toggle mute")) {
            Rift rift = handleRiftInteraction(event);
            if (rift == null) return;
            Member target = event.getTargetMember();
            if (target == null) {
                event.getHook().sendMessage("Invalid target.").queue();
                return;
            }
            User user = target.getUser();
            if (user.isBot() || user.isSystem()) {
                event.getHook().sendMessage("You must do this on a real user!").queue();
                return;
            }
            if (rift.isMuted(user)) {
                rift.unmute(user);
                event.getHook().sendMessage("%s is no longer muted.".formatted(user.getName())).queue();
            } else {
                rift.mute(user);
                event.getHook().sendMessage("%s is now muted.".formatted(user.getName())).queue();
            }
            Main.save();
        }
    }
}
