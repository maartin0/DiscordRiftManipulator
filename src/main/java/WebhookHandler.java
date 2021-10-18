import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.*;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.concurrent.CompletionException;

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
                result.append("> ");
                for (String line : referenced.getContentRaw().replace("\n", "\n> ").split("\n")) {
                    if (!line.contains("> >")) {
                        result.append(line);
                        result.append("\n");
                    }
                }
                result.append(" - ");
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

    public boolean send_webhook_message(String guild_id, String channel_id, User mimic_user, Message msg, String prefix, String message_content) {
        Guild guild = Main.jda.getGuildById(guild_id);
        if (guild == null) return false;

        TextChannel channel = guild.getTextChannelById(channel_id);
        if (channel == null) return false;

        Webhook hook = null;

        List<Webhook> hooks;
        try {
            hooks = channel.retrieveWebhooks().complete();
        } catch (Exception e) {
            return false;
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

        String mesg = message_content;

        if (mesg.length() > 0) {
            WebhookMessageBuilder wmesg = new WebhookMessageBuilder();
            wmesg.setAvatarUrl(mimic_user.getAvatarUrl());
            wmesg.setUsername(name.toString());
            wmesg.setContent(mesg);
            wmesg.setAllowedMentions(new AllowedMentions());

            try {
                client.send(wmesg.build()).join();
            } catch (CompletionException e) {
                return false;
            }

        } return true;
    }

}