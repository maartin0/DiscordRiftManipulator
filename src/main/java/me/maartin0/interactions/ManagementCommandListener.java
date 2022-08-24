package me.maartin0.interactions;

import me.maartin0.Rift;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class ManagementCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getCommandPath()) {
            case "create" -> {
                event.deferReply(true).queue();
                // TODO: Check if rift already exists in that channel
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
                rift.channels.add(
                    new Rift.RiftChannel(
                        new Rift.RiftGuild(
                            guild,
                            event.getUser().getId(),
                            Rift.RiftGuild.generatePrefix(guild.getName()),
                            description.getAsString()
                        ),
                        event.getChannel().asTextChannel()
                    )
                );
                String message = "Success created \"%s\"! Your token is: `%s`. Send this to other servers to connect them together!".formatted(rift.name, rift.token);
                event.getHook().sendMessage(message).queue();
                event.getUser().openPrivateChannel().complete().sendMessage(message).queue();
            } case "join" -> {
                // TODO: Create join method using above functionality, put into Rift and use here and above
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
