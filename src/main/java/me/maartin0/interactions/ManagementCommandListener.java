package me.maartin0.interactions;

import me.maartin0.Rift;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class ManagementCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getCommandPath()) {
            case "create" -> {
                event.deferReply(true).queue();
                if (Rift.lookupFromChannel(event.getGuildChannel()).isPresent()) {
                    event.getHook().sendMessage("A rift already exists in this channel! Exiting...").queue();;
                    return;
                }
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
                rift.addChannel(
                        event.getChannel().asTextChannel(),
                        event.getUser(),
                        description.getAsString()
                );
                String message = "Success created \"%s\"! Your token is: `%s`. Send this to other servers to connect them together!".formatted(rift.name, rift.token);
                event.getHook().sendMessage(message).queue();
                event.getUser().openPrivateChannel().complete().sendMessage(message).queue();
            } case "join" -> {
                event.deferReply(true).queue();
                if (Rift.lookupFromChannel(event.getGuildChannel()).isPresent()) {
                    event.getHook().sendMessage("A rift already exists in this channel! Exiting...").queue();;
                    return;
                }
                OptionMapping token = event.getOption("token");
                if (token == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Guild guild = event.getGuild();
                if (guild == null) {
                    event.getHook().sendMessage("Wrong type!").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.fromToken(token.getAsString());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("Invalid token!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                rift.addChannel(
                        event.getChannel().asTextChannel(),
                        event.getUser(),
                        rift.description
                );
                event.getHook().sendMessage("Success created joined \"%s\"!".formatted(rift.name)).queue();
            } case "leave" -> {
                // TODO: Create leave method, check if rift is empty and clear not needed; autoclear on leave with seperate listener from Main
            } case "modify/global/name" -> {
                // TODO: Simple getRift, save name globally, check if it's the creator guild, no need to save
            } case "modify/global/description" -> {
                // TODO: Simple getRift, save description globally, check if it's the creator guild, no need to save
            } case "modify/prefix" -> {
                // TODO: save prefix in server object, possibly reload rift, suggest reload global descriptions
                // TODO: reload local global? description command
            } case "modify/description" -> {
                // TODO: save description in server object, suggest reload local description
            } case "modify/invite" -> {
                // TODO: save invite in server object, possibly reload rift, suggest reload global descriptions
            }
        }
    }
}
