package me.maartin0;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileProxy;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// TODO: Forward reactions
public class Forwarder {
    public static class Listener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
            new Forwarder(event.getMessage());
        }
    }

    Rift rift;
    Rift.RiftChannel originRiftChannel;
    Message origin;
    public Forwarder(Message origin) {
        Optional<Rift> optionalRift = Rift.lookupFromChannel(origin.getChannel().asTextChannel());
        if (optionalRift.isEmpty()) return;
        this.rift = optionalRift.get();

        this.origin = origin;
        Optional<Rift.RiftChannel> optionalRiftChannel = rift.getRiftChannel(origin.getChannel().asTextChannel());
        if (optionalRiftChannel.isEmpty()) return;
        this.originRiftChannel = optionalRiftChannel.get();

        sendAll();
    }
    void sendAll() {
        rift.channels.stream()
                .filter((Rift.RiftChannel channel) -> channel.channel.getIdLong() != origin.getChannel().getIdLong())
                .forEach((Rift.RiftChannel channel) -> send(channel, getWebhookUsername(), getWebhookMessageContent(), getWebhookMessageAttachments()));
    }
    void send(Rift.RiftChannel channel, String username, String content, Collection<InputStream> attachments) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setAvatarUrl(origin.getAuthor().getAvatarUrl());
        builder.setUsername(username);
        builder.setContent(content);
        attachments.forEach((InputStream stream) -> builder.addFile("attachment.png", stream));
        try (WebhookClient client = WebhookClientBuilder.fromJDA(channel.getWebhook()).build()) {
            client.send(builder.build())
                .whenCompleteAsync((errorMessage, exception) -> {
                    if (exception == null) return;
                    channel.guild.warn(Rift.RiftGuild.WarnReason.SEND_MESSAGE);
                });
        } catch (MissingAccessException e) {
            channel.guild.warn(Rift.RiftGuild.WarnReason.WEBHOOK);
        }
    }
    public static String getWebhookUsername(String prefix, String username) {
        return "%s: %s".formatted(prefix, username);
    }
    String getWebhookUsername() {
        return getWebhookUsername(originRiftChannel.guild.prefix, origin.getAuthor().getName());
    }
    public static String getDiscriminatedUsername(User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator.equals("0000")) return "@ %s".formatted(user.getName());
        else return "@ %s#%s".formatted(user.getName(), discriminator);
    }
    public static String getWebhookMessageContent(Message message) {
        StringBuilder contentBuilder = new StringBuilder();

        // Reply
        Message referencedMessage = message.getReferencedMessage();
        if (referencedMessage != null) {
            contentBuilder.append(
                    referencedMessage.getContentRaw()
                        .lines()
                        .filter((String line) -> !line.startsWith("> "))
                        .map((String line) -> String.format("> %s", line))
                        .collect(Collectors.joining(System.lineSeparator()))
            );
            contentBuilder.append(System.lineSeparator())
                    .append("> ")
                    .append(getDiscriminatedUsername(referencedMessage.getAuthor()))
                    .append(System.lineSeparator());
        }

        // Content
        contentBuilder.append(message.getContentRaw())
                .append(System.lineSeparator());

        // Non-image attachments
        // TODO: Get non-image attachment urls
        return contentBuilder.toString();
    }
    String getWebhookMessageContent() {
        return getWebhookMessageContent(origin);
    }
    static Optional<InputStream> getInputStream(FileProxy proxy) {
        try {
            return Optional.of(proxy.download().get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        }
    }
    List<InputStream> getWebhookMessageAttachments() {
        List<FileProxy> result = new ArrayList<>();
        origin.getStickers()
                .stream()
                .map(StickerItem::getIcon)
                .forEach(result::add);
        origin.getAttachments()
                .stream()
                .map(Message.Attachment::getProxy)
                .forEach(result::add); // TODO: Filter image attachments
        return result.stream()
                .map(Forwarder::getInputStream)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
