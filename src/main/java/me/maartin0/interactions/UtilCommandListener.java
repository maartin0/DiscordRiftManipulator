package me.maartin0.interactions;

import me.maartin0.Rift;
import me.maartin0.Searcher;
import me.maartin0.util.AppConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public class UtilCommandListener extends ListenerAdapter {
    public static @Nullable Rift handleRiftInteraction(CommandInteraction event) {
        event.deferReply(!AppConfig.debug).queue();
        Channel channel = event.getChannel();
        if (!(channel instanceof TextChannel)) {
            event.getHook().sendMessage("Could not find channel, make sure you're doing this in the right channel!").queue();
            return null;
        }
        Optional<Rift> optionalRift = Rift.lookupFromChannel((TextChannel) channel);
        if (optionalRift.isEmpty()) {
            event.getHook().sendMessage("Could not find rift, are you sure you're doing this in the right channel?").queue();
            return null;
        }
        return optionalRift.get();
    }
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        String messageId = event.getMessageId();
        Message message = event.getChannel().retrieveMessageById(messageId).complete();
        if (Rift.lookupFromChannel(message.getChannel().asTextChannel()).isEmpty()) return;
        new Searcher(message)
                .search()
                .exclude(message)
                .get()
                .forEach((Message item) -> item.addReaction(event.getEmoji()).queue());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getCommandPath().equals("purge")) {
            Rift rift = handleRiftInteraction(event);
            if (rift == null) return;
            OptionMapping mapping = event.getOption("number");
            if (mapping == null) {
                event.getHook().sendMessage("Invalid option.").queue();
                return;
            }
            int size = mapping.getAsInt();
            if (size > 100) {
                event.getHook().sendMessage("The provided number was over the maximum number of 100.").queue();
                return;
            }
            rift.channels.forEach((Rift.RiftChannel channel) -> {
                channel.channel.getHistory().retrievePast(size).complete().forEach((Message message) -> {
                    message.delete().queue(success -> {}, failure -> {});
                });
            });
            event.getHook().sendMessage("Deletions scheduled").queue();
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.getCommandPath().equalsIgnoreCase("pin message")) {
            Rift rift = handleRiftInteraction(event);
            if (rift == null) return;
            Collection<Message> result = new Searcher(event.getTarget())
                    .search()
                    .get();
            int size = result.size();
            result.stream()
                    .map(Message::pin)
                    .forEach(RestAction::queue);
            event.getHook().sendMessage("Pinned %s messages (note, this action is not reversible)".formatted(size)).queue();
        } else if (event.getCommandPath().equalsIgnoreCase("delete message")) {
            User messageAuthor = event.getTarget().getAuthor();
            Member requester = event.getMember();
            if (requester == null) {
                event.getHook().sendMessage("Invalid request.").queue();
                return;
            }
            if (
                    (messageAuthor.isBot()
                    || messageAuthor.isSystem()
                    || messageAuthor.getIdLong() != requester.getIdLong())
                    && !requester.hasPermission(Permission.MESSAGE_MANAGE)) {
                    event.getHook().sendMessage("You don't have permission to use this command!").queue();
                    return;
            }
            Rift rift = handleRiftInteraction(event);
            if (rift == null) return;
            Collection<Message> result = new Searcher(event.getTarget())
                    .search()
                    .get();
            int size = result.size();
            result.stream()
                    .map(Message::delete)
                    .forEach(AuditableRestAction::queue);
            event.getHook().sendMessage("Removed %s messages".formatted(size)).queue();
        }
    }
}
