package me.maartin0.interactions;

import me.maartin0.Rift;
import me.maartin0.util.AppConfig;
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
                event.deferReply(!AppConfig.debug).queue();
                if (Rift.lookupFromChannel(event.getChannel().asTextChannel()).isPresent()) {
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
                event.deferReply(!AppConfig.debug).queue();
                if (Rift.lookupFromChannel(event.getChannel().asTextChannel()).isPresent()) {
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
                event.deferReply(!AppConfig.debug).queue();
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                rift.removeChannel(event.getChannel().asTextChannel());
                event.getHook().sendMessage("Success!").queue();
            } case "modify/global/name" -> {
                event.deferReply(!AppConfig.debug).queue();
                OptionMapping name = event.getOption("name");
                if (name == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                if (event.getGuild() == null || !event.getGuild().getId().equals(rift.primaryGuildId)) {
                    event.getHook().sendMessage("You can only do this on the original (primary) guild!").queue();
                    return;
                }
                rift.name = name.getAsString();
                event.getHook().sendMessage("Success! You may also want to run `/reload global description`;").queue();
            } case "modify/global/description" -> {
                event.deferReply(!AppConfig.debug).queue();
                OptionMapping description = event.getOption("description");
                if (description == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                if (event.getGuild() == null || !event.getGuild().getId().equals(rift.primaryGuildId)) {
                    event.getHook().sendMessage("You can only do this on the original (primary) guild!").queue();
                    return;
                }
                rift.description = description.getAsString();
                event.getHook().sendMessage("Success! You may also want to run `/reload description`;").queue();
            } case "modify/prefix" -> {
                event.deferReply(!AppConfig.debug).queue();
                OptionMapping prefix = event.getOption("prefix");
                if (prefix == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                } if (!prefix.getAsString().matches(AppConfig.prefixRegex)) {
                    event.getHook().sendMessage("Invalid prefix").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                Optional<Rift.RiftChannel> optionalRiftChannel = rift.getRiftChannel(event.getChannel().asTextChannel());
                if (optionalRiftChannel.isEmpty()) {
                    event.getHook().sendMessage("Unable to update prefix.").queue();
                    return;
                }
                Rift.RiftChannel riftChannel = optionalRiftChannel.get();
                riftChannel.guild.prefix = prefix.getAsString();
                event.getHook().sendMessage("Success! You may also want to to run `/reload (global) description`;").queue();
            } case "modify/description" -> {
                event.deferReply(!AppConfig.debug).queue();
                OptionMapping description = event.getOption("description");
                if (description == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                Optional<Rift.RiftChannel> optionalRiftChannel = rift.getRiftChannel(event.getChannel().asTextChannel());
                if (optionalRiftChannel.isEmpty()) {
                    event.getHook().sendMessage("Unable to update description.").queue();
                    return;
                }
                Rift.RiftChannel riftChannel = optionalRiftChannel.get();
                riftChannel.guild.description = description.getAsString();
                event.getHook().sendMessage("Success! You may also want to to run `/reload description`;").queue();
            } case "modify/invite" -> {
                event.deferReply(!AppConfig.debug).queue();
                OptionMapping invite = event.getOption("code");
                if (invite == null) {
                    event.getHook().sendMessage("Unable to get provided options").queue();
                    return;
                }
                Optional<Rift> riftOptional = Rift.lookupFromChannel(event.getChannel().asTextChannel());
                if (riftOptional.isEmpty()) {
                    event.getHook().sendMessage("There's no rift here!").queue();
                    return;
                }
                Rift rift = riftOptional.get();
                Optional<Rift.RiftChannel> optionalRiftChannel = rift.getRiftChannel(event.getChannel().asTextChannel());
                if (optionalRiftChannel.isEmpty()) {
                    event.getHook().sendMessage("Unable to update invite.").queue();
                    return;
                }
                Rift.RiftChannel riftChannel = optionalRiftChannel.get();
                riftChannel.guild.invite = "https://discord.gg/%s".formatted(invite.getAsString());
                event.getHook().sendMessage("Success! You may also want to to run `/reload (global) description`;").queue();
            }
        }
    }
}
