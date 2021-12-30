import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

public class WebhookHandler {
    String replace_mentions(String msg, List<User> mentions) {
        String result = msg;
        for (User u : mentions) {
            String id = u.getId();
            String replacement_text = "@" + u.getName() + "#" + u.getDiscriminator();

            result = result.replace(
                    "<@!" + id + ">",
                    replacement_text
            ).replace(
                    "<@" + id + ">",
                    replacement_text
            );
        }

        return result.replace("@", "@ ");
    }

    public String get_message_content(Message msg) {
        StringBuilder result = new StringBuilder();
        if (!msg.getContentRaw().equals("")) {
            Message referenced = msg.getReferencedMessage();
            if (referenced != null) {
                boolean last_was_reply = false;
                for (String line : referenced.getContentRaw().split("\n")) {
                    if (line.startsWith("> ")) {
                        last_was_reply = true;
                    } else if (last_was_reply) {
                        last_was_reply = false;
                    } else if (!line.equals("")) {
                        result.append("> ");
                        result.append(line);
                    }
                }

                for (Message.Attachment a : referenced.getAttachments()) {
                    result.append("\n> ");
                    result.append(a.getUrl());
                }

                result.append("\n - ");
                result.append(referenced.getAuthor().getName());
                result.append("#");
                result.append(referenced.getAuthor().getDiscriminator());
                result.append("\n");
            }

            result.append(msg.getContentRaw());
        }

        for (Message.Attachment a : msg.getAttachments()) {
            result.append("\n");
            result.append(a.getUrl());
        }

        return replace_mentions(result.toString(), msg.getMentionedUsers());
    }

    public void send_webhook_message(String guild_id, String channel_id, User mimic_user, String prefix, String message_content) {
        Guild guild = Main.jda.getGuildById(guild_id);
        if (guild == null) return;

        TextChannel channel = guild.getTextChannelById(channel_id);
        if (channel == null) return;

        Webhook hook = null;

        List<Webhook> hooks;
        try {
            hooks = channel.retrieveWebhooks().complete();
        } catch (Exception e) {
            return;
        }

        for (Webhook webhook : hooks) {
            if (webhook.getName().equals("Rift Handler")) {
                hook = webhook;
                break;
            }
        }

        if (hook == null) {
            hook = channel.createWebhook("Rift Handler").complete();
        }

        StringBuilder name = new StringBuilder();
        name.append("[");
        name.append(prefix);
        name.append("] ");
        name.append(mimic_user.getName());

        JDAWebhookClient client = WebhookClientBuilder.fromJDA(hook).buildJDA();

        if (message_content.length() > 0) {
            WebhookMessageBuilder webhookMessageBuilder = new WebhookMessageBuilder();
            webhookMessageBuilder.setAvatarUrl(mimic_user.getAvatarUrl());
            webhookMessageBuilder.setUsername(name.toString());
            webhookMessageBuilder.setContent(message_content);
            webhookMessageBuilder.setAllowedMentions(new AllowedMentions());

            client.send(webhookMessageBuilder.build())
                .whenCompleteAsync(
                        (message, exception) -> Main.listener.handle_warn(guild_id, channel_id, (exception != null))
                );

        }
    }

}