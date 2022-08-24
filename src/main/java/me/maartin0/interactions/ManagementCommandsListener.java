package me.maartin0.interactions;

import me.maartin0.Rift;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class ManagementCommandsListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getCommandPath()) {
            case "create" -> {
                event.deferReply(true).queue();
                OptionMapping name = event.getOption("name");
                OptionMapping description = event.getOption("description");
                if (name == null || description == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Guild guild = event.getGuild();
                if (guild == null) {
                    event.getHook().sendMessage("Wrong type!").queue();
                    return;
                }
                Rift rift = new Rift(
                        UUID.randomUUID().toString(),
                        name.getAsString(),
                        description.getAsString(),
                        guild.getId(),
                        new ArrayList<>()
                );
                String message = "Success created \"%s\"! Your token is: `%s`. Send this to other servers to connect them together!".formatted(rift.name, rift.token);
                event.getHook().sendMessage(message).queue();
                event.getUser().openPrivateChannel().complete().sendMessage(message).queue();
            } case "join" -> {

            } case "leave" -> {

            } case "modify/global/name" -> {

            } case "modify/global/description" -> {

            } case "modify/prefix" -> {

            } case "modify/description" -> {

            } case "modify/invite" -> {

            }
        }
    }
}
