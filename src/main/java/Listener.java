import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Listener extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Main.update_status();
    }

    public void command_reply(SlashCommandEvent event, String reply) {
        event.getHook().sendMessage(reply).setEphemeral(true).queue();
    }

    public void handle_warn(String guild_id, String channel_id, boolean warn) {
        if (warn) {
            Guild g = Main.jda.getGuildById(guild_id);
            if (g == null) return;

            TextChannel c = g.getTextChannelById(channel_id);
            if (c == null) return;

            warn_guild_owner(g, c);
        } else if (Main.warned_servers.containsKey(guild_id) && Main.warned_servers.get(guild_id).contains(channel_id)) {
            List<String> updated_list = Main.warned_servers.get(guild_id);

            if (updated_list.size() == 1) {
                Main.warned_servers.remove(guild_id);
            } else {
                updated_list.remove(channel_id);
            }
        }
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getGuild() == null || event.getUser().isBot() || event.getUser().isSystem()) { return; }

        event.deferReply(true).queue();

        if (!check_channel_for_permissions(event)) return;

        switch (event.getName()) {
            case "create" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                create_rift(event);
            }
            case "leave" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                leave_rift(event);
            }
            case "delete-message" -> {
                if (has_invalid_permissions(event, Permission.MESSAGE_MANAGE)) { return; }
                delete_message(event);
            }
            case "global_modify" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                modify_rift(event);
            }
            case "modify" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                modify_channel(event);
            }
            case "set_prefix" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                set_prefix(event);
            }
            case "set_description" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                set_description(event);
            }
            case "set_invite" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                set_invite(event);
            }
            case "join" -> {
                if (has_invalid_permissions(event, Permission.ADMINISTRATOR)) { return; }
                join_rift(event);
            }
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Main.update_status();

        StringBuilder message = new StringBuilder();
        message.append("I was just added to the guild called '");
        message.append(event.getGuild().getName());
        message.append("'. The server is owned by ");
        User owner = event.getGuild().retrieveOwner().complete().getUser();
        message.append(owner.getName());
        message.append("#");
        message.append(owner.getDiscriminator());

        for (String user_id : Main.debug_administrators) {
            Main.jda.retrieveUserById(user_id).complete().openPrivateChannel().complete().sendMessage(message.toString()).queue();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Main.update_status();

        Main.riftData.clear_guild_rifts(event.getGuild().getId());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE) || event.getAuthor().isBot() || event.getAuthor().isSystem()) { return; }

        String gid = event.getGuild().getId();
        String cid = event.getChannel().getId();

        if (!Main.riftData.channel_has_rift(gid, cid)) { return; }

        String token = Main.riftData.servers.getAsJsonObject(gid).get(cid).getAsString();

        HashMap<String, List<String>> channels = Main.riftData.get_rift_channels(token);

        String prefix = Main.riftData.tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(gid).get("prefix").getAsString();

        User author = event.getAuthor();
        String message_content = Main.webhookHandler.get_message_content(event.getMessage());

        channels.keySet().forEach((String guild_id) -> channels.get(guild_id).forEach((String channel_id) -> {
            if (guild_id.equals(gid) && channel_id.equals(cid)) return;
            Main.webhookHandler.send_webhook_message(guild_id, channel_id, author, prefix, message_content);
        }));
    }

    boolean has_invalid_permissions(SlashCommandEvent event, Permission permission) {
        if (Objects.requireNonNull(event.getMember()).hasPermission(permission) || Main.debug_administrators.contains(event.getUser().getId())) {
            return false;
        } else {
            command_reply(event, "You don't have permission to use this command!");
            return true;
        }
    }

    boolean refresh_channel_description(String guild_id, String channel_id) {
        String description = Main.riftData.get_description(guild_id, channel_id);

        if (description == null) return false;

        try {
            Main.jda.getGuildById(guild_id).getTextChannelById(channel_id).getManager().setTopic(description).queue();
        } catch (NullPointerException e) {
            return false;
        }

        return true;
    }

    void refresh_rift_descriptions(String token) {
        HashMap<String, List<String>> channels = Main.riftData.get_rift_channels(token);
        for (String server_id : channels.keySet()) {
            for (String channel_id : channels.get(server_id)) {
                refresh_channel_description(server_id, channel_id);
            }
        }
    }

    boolean check_channel_for_permissions(SlashCommandEvent event) {
        Guild guild = event.getGuild();
        GuildChannel channel = event.getGuildChannel();

        if (guild == null) return false;

        String self_id = Main.jda.getSelfUser().getId();
        Member self = guild.getMemberById(self_id);

        if (self == null) return false;

        for (Permission permission : Main.required_permissions) {
            if (!self.hasPermission(channel, permission)) {
                command_reply(event, "I need the "+permission.getName()+" permission to function correctly! Exiting...");
                return false;
            }
        }

        return true;
    }

    void warn_guild_owner(Guild g, TextChannel c) {
        String guild_id = g.getId();
        String channel_id = c.getId();

        if (Main.warned_servers.containsKey(guild_id) && Main.warned_servers.get(guild_id).contains(channel_id)) return;

        String message = "Your guild with the name '" +
                g.getName() +
                "' has a rift setup on the channel called '" +
                c.getName() +
                "'.\nThe bot was unable to send a message to that channel due to a lack of permissions.\nPlease check the permissions and test the connection.\nTo stop these messages either block the bot, kick the bot from your guild, or remove the rift from the channel using /leave.";
        g.retrieveOwner().complete().getUser().openPrivateChannel().complete().sendMessage(message).queue();

        if (Main.warned_servers.containsKey(guild_id)) {
            Main.warned_servers.get(guild_id).add(channel_id);
        } else {
            Main.warned_servers.put(guild_id, Collections.singletonList(channel_id));
        }
    }

    boolean delete_message_from_channel(TextChannel channel, Message message) {
        List<Message> history = channel.getHistory().retrievePast(100).complete();
        String content = Main.webhookHandler.get_message_content(message);

        for (Message m : history) {
            if (m.getContentRaw().equals(content)) {
                m.delete().queue();
                return true;
            }
        }

        return false;
    }

    void create_rift(SlashCommandEvent event) {
        if (Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "A rift already exists in this channel!");
            return;
        }

        String name;
        String description;

        try {
            name = event.getOption("name").getAsString();
            description = event.getOption("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message + " Error: Unable to get command arguments.");
            return;
        }

        String token = Main.riftData.create_rift_data(name, description, event.getUser().getId(), Objects.requireNonNull(event.getGuild()).getId(), event.getChannel().getId());
        if (token == null) {
            command_reply(event, Main.unknown_error_message);
            return;
        }
        boolean result = refresh_channel_description(event.getGuild().getId(), event.getChannel().getId());
        if (!result) {
            command_reply(event, "Unable to update channel description, continuing anyway, your rift token is: `" + token + "`. Send this to other servers to create your rift!");
            return;
        }

        command_reply(event, "Success! Your rift token is: `" + token + "`. Send this to other servers to create your rift!");

        event.getUser().openPrivateChannel().complete().sendMessage("You created a rift called \"" + name + "\". It's token is: `" + token + "` Keep this safe! Anyone who gets access to it can join your rift!").queue();
    }

    void join_rift(SlashCommandEvent event) {
        if (Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "A rift already exists in this channel!");
            return;
        }

        String token;
        try {
            token = event.getOption("token").getAsString();
        } catch (NullPointerException e) {
            command_reply(event, Main.unknown_error_message + "Error: Unable to get command arguments.");
            return;
        }

        String description = Main.riftData.add_rift_data(token, event.getUser().getId(), event.getGuild().getId(), event.getChannel().getId());

        if (description == null) {
            command_reply(event, "Error: Invalid token!");
            return;
        }

        String name = Main.riftData.get_rift_name(token);

        if (name == null) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        refresh_rift_descriptions(token);

        command_reply(event, "You have successfully joined the \""+name+"\" rift!");
    }

    void delete_message(SlashCommandEvent event) {
        String gid = event.getGuild().getId();
        String cid = event.getChannel().getId();

        String message_id;
        try {
            message_id = event.getOption("message_id").getAsString();
        } catch (NullPointerException e) {
            command_reply(event, Main.unknown_error_message + "Error: Unable to get message arguments.");
            return;
        }

        MessageHistory history = event.getChannel().getHistory();
        history.retrievePast(100).complete();
        Message message = null;

        for (Message message1 : history.getRetrievedHistory()) {
            if (message1.getId().equals(message_id)) {
                message = message1;
                break;
            }
        }

        if (message == null) {
            command_reply(event, "Invalid message ID (or the message is too old)");
            return;
        }

        if (!Main.riftData.channel_has_rift(gid, cid)) { return; }

        String token = Main.riftData.servers.getAsJsonObject(gid).get(cid).getAsString();

        HashMap<String, List<String>> channels = Main.riftData.get_rift_channels(token);

        int count = 1;
        int total = 1;

        for (String guild_id : channels.keySet()) {
            Guild g = Main.jda.getGuildById(guild_id);
            if (g == null) continue;
            for (String channel_id : channels.get(guild_id)) {
                if (!(guild_id.equals(gid) && channel_id.equals(cid))) {
                    TextChannel t = g.getTextChannelById(channel_id);
                    if (t == null) continue;

                    boolean result = delete_message_from_channel(t, message);
                    total += 1;
                    if (result) count += 1;
                }
            }
        }

        message.delete().queue();

        String reply = "Successfully deleted " +
                count +
                "/" +
                total +
                " instances of that message.";

        command_reply(event, reply);
    }

    void leave_rift(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(Objects.requireNonNull(event.getGuild()).getId(), event.getChannel().getId())) {
            command_reply(event, "This text channel doesn't have any rifts associated with it!");
            return;
        }

        if (!Main.riftData.delete_rift_data(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        event.getTextChannel().getManager().setTopic("").queue();

        command_reply(event, "The rift has been successfully removed from this channel!");
    }

    void modify_channel(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "This channel doesn't contain a multiverse!");
            return;
        }

        String new_prefix = null;
        String new_description = null;
        String new_invite = null;

        try {
            if (event.getOption("prefix") != null) new_prefix = event.getOption("prefix").getAsString();
            if (event.getOption("description") != null) new_description = event.getOption("description").getAsString();
            if (event.getOption("invite_code") != null) new_invite = event.getOption("invite_code").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message);
            return;
        }

        if (new_invite == null && new_description == null && new_prefix == null) {
            command_reply(event, "No arguments provided!");
            return;
        }

        if (new_prefix != null && Main.riftData.is_invalid_prefix(new_prefix)) {
            command_reply(event, "Invalid prefix! The maximum length is "+Main.max_prefix_length+" and the prefix can only include A-z, 0-9, \"-\", \"_\", and accented characters.");
            return;
        }

        if (new_description != null && (new_description.length() < 1 || new_description.length() > Main.max_description_length)) {
            command_reply(event, "The description length must be in the range of 1 ≤ " + "x ≤ " + Main.max_description_length);
            return;
        }

        if (new_invite != null && (new_invite.contains("/") || new_invite.contains("\\") || new_invite.contains(":"))) {
            command_reply(event, "Invalid invite code. Make sure you're supplying the **code**, not the **URL**.");
            return;
        }

        if (!Main.riftData.modify_rift_data(event.getGuild().getId(), event.getChannel().getId(), new_prefix, new_description, new_invite)) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refresh_rift_descriptions(token);

        command_reply(event, "Successfully Modified!");
    }

    void set_prefix(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "This channel doesn't contain a rift!");
            return;
        }

        String new_prefix = null;

        try {
            if (event.getOption("prefix") != null) new_prefix = event.getOption("prefix").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message);
            return;
        }

        if (new_prefix == null) {
            command_reply(event, "No arguments provided!");
            return;
        }

        if (Main.riftData.is_invalid_prefix(new_prefix)) {
            command_reply(event, "Invalid prefix! The maximum length is "+Main.max_prefix_length+" and the prefix can only include A-z, 0-9, \"-\", \"_\", and accented characters.");
            return;
        }

        if (!Main.riftData.modify_rift_data(event.getGuild().getId(), event.getChannel().getId(), new_prefix, null, null)) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refresh_rift_descriptions(token);

        command_reply(event, "Successfully Modified!");
    }

    void set_description(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "This channel doesn't contain a rift!");
            return;
        }

        String new_description = null;

        try {
            if (event.getOption("description") != null) new_description = event.getOption("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message);
            return;
        }

        if (new_description == null) {
            command_reply(event, "No arguments provided!");
            return;
        }

        if (new_description.length() < 1 || new_description.length() > Main.max_description_length) {
            command_reply(event, "The description length must be in the range of 1 ≤ " + "x ≤ " + Main.max_description_length);
            return;
        }

        if (!Main.riftData.modify_rift_data(event.getGuild().getId(), event.getChannel().getId(), null, new_description, null)) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refresh_channel_description(event.getGuild().getId(), event.getChannel().getId());

        command_reply(event, "Successfully Modified!");
    }

    void set_invite(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "This channel doesn't contain a rift!");
            return;
        }

        String new_invite = null;

        try {
            if (event.getOption("invite_code") != null) new_invite = event.getOption("invite_code").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message);
            return;
        }

        if (new_invite == null) {
            command_reply(event, "No arguments provided!");
            return;
        }

        if (new_invite.contains("/") || new_invite.contains("\\") || new_invite.contains(":")) {
            command_reply(event, "Invalid invite code. Make sure you're supplying the **code**, not the **URL**.");
            return;
        }

        if (!Main.riftData.modify_rift_data(event.getGuild().getId(), event.getChannel().getId(), null, null, new_invite)) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refresh_rift_descriptions(token);

        command_reply(event, "Successfully Modified!");
    }

    void modify_rift(SlashCommandEvent event) {
        if (!Main.riftData.channel_has_rift(event.getGuild().getId(), event.getChannel().getId())) {
            command_reply(event, "This channel doesn't contain a rift!");
            return;
        }

        String token;
        String guild_id;

        try {
            token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();
            guild_id = Main.riftData.tokens.getAsJsonObject(token).get("creator_guild").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            command_reply(event, Main.unknown_error_message + " Error: Unable to get command arguments.");
            return;
        }

        if (!event.getGuild().getId().equals(guild_id)) {
            command_reply(event, "Your guild doesn't have permission to run this command!");
            return;
        }

        String name = event.getOption("name").getAsString();
        String description = event.getOption("description").getAsString();

        if (!Main.riftData.modify_global_rift_data(token, name, description)) {
            command_reply(event, Main.unknown_error_message);
            return;
        }

        refresh_rift_descriptions(token);

        command_reply(event, "Successfully modified rift!");
    }
}
