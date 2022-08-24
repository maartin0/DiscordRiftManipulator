package old;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.MissingAccessException;

import java.util.List;
import java.util.Objects;

public class WebhookHandler {
    public JDAWebhookClient getChannelWebhook(TextChannel channel) {
        List<Webhook> hooks;
        try {
            hooks = channel.retrieveWebhooks().complete();
        } catch (MissingAccessException e) {
            return null;
        }

        Webhook hook;
        if (hooks.size() == 0) hook = channel.createWebhook("Rift Handler").complete();
        else hook = hooks.get(0);

        return WebhookClientBuilder.fromJDA(hook).buildJDA();
    }

    WebhookMessage getWebhookMessage(Message message, TextChannel origin) {
        String messageContent = MessageFormatter.getMessageContent(message);
        if (messageContent.length() == 0) return null;

        Member member = message.getMember();
        if (Objects.isNull(member)) return null;

        WebhookMessageBuilder webhookMessageBuilder = new WebhookMessageBuilder();
        webhookMessageBuilder.setAvatarUrl(member.getUser().getAvatarUrl());
        webhookMessageBuilder.setUsername(MessageFormatter.getPrefixedName(origin, member));
        webhookMessageBuilder.setContent(messageContent);

        webhookMessageBuilder.setAllowedMentions(new AllowedMentions());

        return webhookMessageBuilder.build();
    }

    public void sendWebhookMessages(TextChannel origin, Message message, List<TextChannel> targets) {
        WebhookMessage webhookMessage = getWebhookMessage(message, origin);
        if (Objects.isNull(webhookMessage)) return;
        targets.forEach((TextChannel channel) -> sendWebhookMessage(channel, webhookMessage));
    }

    void sendWebhookMessage(TextChannel channel, WebhookMessage message) {
        JDAWebhookClient client = getChannelWebhook(channel);
        if (client == null) return;

        client.send(message)
            .whenCompleteAsync(
                    (errorMessage, exception) -> Main.listener.handleWarn(channel, (exception != null))
            );
    }

    public void editWebhookMessages(Message message) {
        MessageLocator.getAllMessages(message, true).forEach((message1 -> editWebhookMessage(message, message1)));
    }

    void editWebhookMessage(Message newMessage, Message oldMessage) {
        if (!oldMessage.isWebhookMessage()) return;

        JDAWebhookClient client = getChannelWebhook(oldMessage.getChannel().asTextChannel());
        if (Objects.isNull(client)) return;

        WebhookMessage webhookMessage = getWebhookMessage(newMessage, newMessage.getChannel().asTextChannel());
        client.edit(oldMessage.getIdLong(), webhookMessage);
    }

}