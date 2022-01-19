import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

public class WebhookHandler {
    public Webhook getChannelWebhook(TextChannel channel) throws RuntimeException {
        List<Webhook> hooks = channel.retrieveWebhooks().complete();

        return hooks
                .stream()
                .filter(webhook -> webhook.getName().equals("Rift Handler"))
                .findFirst()
                .orElse(channel.createWebhook("Rift Handler").complete());
    }

    public void sendWebhookMessages(TextChannel origin, Message message, List<TextChannel> targets) {
        String messageContent = MessageFormatter.getMessageContent(message);
        if (messageContent.length() == 0) return;

        Member member = message.getMember();

        WebhookMessageBuilder webhookMessageBuilder = new WebhookMessageBuilder();
        webhookMessageBuilder.setAvatarUrl(member.getUser().getAvatarUrl());
        webhookMessageBuilder.setUsername(MessageFormatter.getPrefixedName(origin, member));
        webhookMessageBuilder.setContent(messageContent);

        webhookMessageBuilder.setAllowedMentions(new AllowedMentions());

        WebhookMessage webhookMessage = webhookMessageBuilder.build();

        targets.forEach((TextChannel channel) -> sendWebhookMessage(channel, webhookMessage));
    }

    public void sendWebhookMessage(TextChannel channel, WebhookMessage message) {
        Webhook hook;
        try {
            hook = getChannelWebhook(channel);
        } catch (RuntimeException e) {
            return;
        }

        WebhookClientBuilder.fromJDA(hook).buildJDA().send(message)
            .whenCompleteAsync(
                    (errorMessage, exception) -> Main.listener.handleWarn(channel, (exception != null))
            );
    }

}