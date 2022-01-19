import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Listener extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Main.update_status();
    }

    public void commandReply(SlashCommandEvent event, String reply) {
        event.getHook().sendMessage(reply).setEphemeral(true).queue();
    }

    public void handleWarn(TextChannel c, boolean warn) {
        String guildID = c.getGuild().getId();
        String channelID = c.getId();
        if (warn) {
            warnGuildOwner(c);
        } else if (Main.warned_servers.containsKey(guildID) && Main.warned_servers.get(guildID).contains(channelID)) {
            List<String> updated_list = Main.warned_servers.get(guildID);

            if (updated_list.size() == 1) {
                Main.warned_servers.remove(guildID);
            } else {
                updated_list.remove(channelID);
            }
        }
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getGuild() == null || event.getUser().isBot() || event.getUser().isSystem()) { return; }

        event.deferReply(true).queue();

        if (!checkChannelForPermissions(event)) return;

        switch (event.getName()) {
            case "create" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                createRift(event);
            }
            case "leave" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                leaveRift(event);
            }
            case "delete-message" -> {
                if (hasInvalidPermissions(event, Permission.MESSAGE_MANAGE)) { return; }
                deleteMessage(event);
            }
            case "global_modify" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                modifyRift(event);
            }
            case "modify" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                modifyChannel(event);
            }
            case "set_prefix" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                setPrefix(event);
            }
            case "set_description" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                setDescription(event);
            }
            case "set_invite" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                setInvite(event);
            }
            case "join" -> {
                if (hasInvalidPermissions(event, Permission.ADMINISTRATOR)) { return; }
                joinRift(event);
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

        Main.riftData.clearGuildRifts(event.getGuild().getId());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE) || event.getAuthor().isBot() || event.getAuthor().isSystem()) { return; }

        TextChannel origin = event.getTextChannel();
        String token = Main.riftData.getChannelToken(origin);
        if (Objects.isNull(token)) return;

        Main.webhookHandler.sendWebhookMessages(event.getTextChannel(), event.getMessage(), MessageLocator.getRiftChannels(token, origin));
    }

    boolean hasInvalidPermissions(SlashCommandEvent event, Permission permission) {
        if (Objects.requireNonNull(event.getMember()).hasPermission(permission) || Main.debug_administrators.contains(event.getUser().getId())) {
            return false;
        } else {
            commandReply(event, "You don't have permission to use this command!");
            return true;
        }
    }

    boolean refreshChannelDescription(String guild_id, String channel_id) {
        String description = Main.riftData.getDescription(guild_id, channel_id);

        if (description == null) return false;

        try {
            Main.jda.getGuildById(guild_id).getTextChannelById(channel_id).getManager().setTopic(description).queue();
        } catch (NullPointerException e) {
            return false;
        }

        return true;
    }

    void refreshRiftDescriptions(String token) {
        HashMap<String, List<String>> channels = Main.riftData.getRiftChannels(token);
        for (String server_id : channels.keySet()) {
            for (String channel_id : channels.get(server_id)) {
                refreshChannelDescription(server_id, channel_id);
            }
        }
    }

    boolean checkChannelForPermissions(SlashCommandEvent event) {
        Guild guild = event.getGuild();
        GuildChannel channel = event.getGuildChannel();

        if (guild == null) return false;

        String self_id = Main.jda.getSelfUser().getId();
        Member self = guild.getMemberById(self_id);

        if (self == null) return false;

        for (Permission permission : Main.required_permissions) {
            if (!self.hasPermission(channel, permission)) {
                commandReply(event, "I need the %s permission to function correctly! Exiting...".formatted(permission.getName()));
                return false;
            }
        }

        return true;
    }

    void warnGuildOwner(TextChannel c) {
        Guild g = c.getGuild();
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

    void createRift(SlashCommandEvent event) {
        if (Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "A rift already exists in this channel!");
            return;
        }

        String name;
        String description;

        try {
            name = event.getOption("name").getAsString();
            description = event.getOption("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE + " Error: Unable to get command arguments.");
            return;
        }

        String token = Main.riftData.createRiftData(name, description, event.getUser().getId(), Objects.requireNonNull(event.getGuild()).getId(), event.getChannel().getId());
        if (token == null) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }
        boolean result = refreshChannelDescription(event.getGuild().getId(), event.getChannel().getId());
        if (!result) {
            commandReply(event, "Unable to update channel description, continuing anyway, your rift token is: `" + token + "`. Send this to other servers to create your rift!");
            return;
        }

        commandReply(event, "Success! Your rift token is: `" + token + "`. Send this to other servers to create your rift!");

        event.getUser().openPrivateChannel().complete().sendMessage("You created a rift called \"" + name + "\". It's token is: `" + token + "` Keep this safe! Anyone who gets access to it can join your rift!").queue();
    }

    void joinRift(SlashCommandEvent event) {
        if (Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "A rift already exists in this channel!");
            return;
        }

        String token;
        try {
            token = event.getOption("token").getAsString();
        } catch (NullPointerException e) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE + "Error: Unable to get command arguments.");
            return;
        }

        String description = Main.riftData.addRiftData(token, event.getUser().getId(), event.getGuild().getId(), event.getChannel().getId());

        if (description == null) {
            commandReply(event, "Error: Invalid token!");
            return;
        }

        String name = Main.riftData.getRiftName(token);

        if (name == null) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        refreshRiftDescriptions(token);

        commandReply(event, "You have successfully joined the \""+name+"\" rift!");
    }

    void deleteMessage(SlashCommandEvent event) {
        String message_id = Objects.requireNonNull(event.getOption("message_id")).getAsString();

        Message origin = MessageLocator.getMessageFromChannel(
                (Message message) -> message.getId().equals(message_id),
                event.getTextChannel()
        );

        if (Objects.isNull(origin)) {
            commandReply(event, String.format("Could not find message, is the ID correct or is it older than %s messages?", Main.BACKWARD_SEARCH_MAX));
            return;
        }

        MessageLocator.getAllMessages(origin).stream().map(Message::delete).forEach(AuditableRestAction::queue);

        commandReply(event, "Successfully deleted the supplied message.");
    }

    void leaveRift(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This text channel doesn't have any rifts associated with it!");
            return;
        }

        if (!Main.riftData.deleteRiftData(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        event.getTextChannel().getManager().setTopic("").queue();

        commandReply(event, "The rift has been successfully removed from this channel!");
    }

    void modifyChannel(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This channel doesn't contain a multiverse!");
            return;
        }

        String newPrefix = null;
        String newDescription = null;
        String newInvite = null;

        try {
            if (event.getOption("prefix") != null) newPrefix = event.getOption("prefix").getAsString();
            if (event.getOption("description") != null) newDescription = event.getOption("description").getAsString();
            if (event.getOption("invite_code") != null) newInvite = event.getOption("invite_code").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        if (newInvite == null && newDescription == null && newPrefix == null) {
            commandReply(event, "No arguments provided!");
            return;
        }

        if (newPrefix != null && Main.riftData.isInvalidPrefix(newPrefix)) {
            commandReply(event, "Invalid prefix! The maximum length is "+Main.MAX_PREFIX_LENGTH +" and the prefix can only include A-z, 0-9, \"-\", \"_\", and accented characters.");
            return;
        }

        if (newDescription != null && (newDescription.length() < 1 || newDescription.length() > Main.MAX_DESCRIPTION_LENGTH)) {
            commandReply(event, "The description length must be in the range of 1 ≤ " + "x ≤ " + Main.MAX_DESCRIPTION_LENGTH);
            return;
        }

        if (newInvite != null && (newInvite.contains("/") || newInvite.contains("\\") || newInvite.contains(":"))) {
            commandReply(event, "Invalid invite code. Make sure you're supplying the **code**, not the **URL**.");
            return;
        }

        if (!Main.riftData.modifyRiftData(event.getGuild().getId(), event.getChannel().getId(), newPrefix, newDescription, newInvite)) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refreshRiftDescriptions(token);

        commandReply(event, "Successfully Modified!");
    }

    void setPrefix(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This channel doesn't contain a rift!");
            return;
        }

        String newPrefix = null;

        try {
            if (event.getOption("prefix") != null) newPrefix = event.getOption("prefix").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        if (newPrefix == null) {
            commandReply(event, "No arguments provided!");
            return;
        }

        if (Main.riftData.isInvalidPrefix(newPrefix)) {
            commandReply(event, "Invalid prefix! The maximum length is " + Main.MAX_PREFIX_LENGTH + " and the prefix can only include A-z, 0-9, \"-\", \"_\", and accented characters.");
            return;
        }

        if (!Main.riftData.modifyRiftData(event.getGuild().getId(), event.getChannel().getId(), newPrefix, null, null)) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

        refreshRiftDescriptions(token);

        commandReply(event, "Successfully Modified!");
    }

    void setDescription(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This channel doesn't contain a rift!");
            return;
        }

        String newDescription = null;

        try {
            if (event.getOption("description") != null) newDescription = event.getOption("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        if (newDescription == null) {
            commandReply(event, "No arguments provided!");
            return;
        }

        if (newDescription.length() < 1 || newDescription.length() > Main.MAX_DESCRIPTION_LENGTH) {
            commandReply(event, "The description length must be in the range of 1 ≤ " + "x ≤ " + Main.MAX_DESCRIPTION_LENGTH);
            return;
        }

        if (!Main.riftData.modifyRiftData(event.getGuild().getId(), event.getChannel().getId(), null, newDescription, null)) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        refreshChannelDescription(event.getGuild().getId(), event.getChannel().getId());

        commandReply(event, "Successfully Modified!");
    }

    void setInvite(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This channel doesn't contain a rift!");
            return;
        }

        String newInvite = null;

        try {
            if (event.getOption("invite_code") != null) newInvite = event.getOption("invite_code").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        if (newInvite == null) {
            commandReply(event, "No arguments provided!");
        } else if (newInvite.contains("/") || newInvite.contains("\\") || newInvite.contains(":")) {
            commandReply(event, "Invalid invite code. Make sure you're supplying the **code**, not the **URL**.");
        } else if (!Main.riftData.modifyRiftData(event.getGuild().getId(), event.getChannel().getId(), null, null, newInvite)) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
        } else {
            String token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();

            refreshRiftDescriptions(token);

            commandReply(event, "Successfully Modified!");
        }

    }

    void modifyRift(SlashCommandEvent event) {
        if (!Main.riftData.channelHasRift(event.getGuild().getId(), event.getChannel().getId())) {
            commandReply(event, "This channel doesn't contain a rift!");
            return;
        }

        String token;
        String guildID;

        try {
            token = Main.riftData.servers.getAsJsonObject(event.getGuild().getId()).get(event.getChannel().getId()).getAsString();
            guildID = Main.riftData.tokens.getAsJsonObject(token).get("creator_guild").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE + " Error: Unable to get command arguments.");
            return;
        }

        if (!event.getGuild().getId().equals(guildID)) {
            commandReply(event, "Your guild doesn't have permission to run this command!");
            return;
        }

        String name = event.getOption("name").getAsString();
        String description = event.getOption("description").getAsString();

        if (!Main.riftData.modifyGlobalRiftData(token, name, description)) {
            commandReply(event, Main.UNKNOWN_ERROR_MESSAGE);
            return;
        }

        refreshRiftDescriptions(token);

        commandReply(event, "Successfully modified rift!");
    }
}
