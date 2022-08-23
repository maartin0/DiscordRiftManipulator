import com.google.gson.JsonArray;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.*;

public class Main {

    public static final String UNKNOWN_ERROR_MESSAGE = "An unknown error occurred. Please check your details, try again or contact an administrator.";

    public static final Integer MAX_PREFIX_LENGTH = 10;
    public static final Integer MAX_DESCRIPTION_LENGTH = 200;
    public static String valid_prefix_regex;

    public static final Integer BACKWARD_SEARCH_SKIP = 50;
    public static final Integer BACKWARD_SEARCH_MAX = 300;

    public static List<String> debug_administrators = new ArrayList<>();

    public final static List<Permission> required_permissions = Arrays.asList(Permission.VIEW_CHANNEL, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL, Permission.MANAGE_WEBHOOKS, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI);

    public static HashMap<String, List<String>> warned_servers = new HashMap<>();

    public static JDA jda;
    public static RiftData riftData;
    public static WebhookHandler webhookHandler;
    public static ConfigHandler configHandler;
    public static Listener listener;

    static void updateCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("create", "Creates a new rift with the supplied name and description. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "name", "The name of the rift.").setRequired(true))
                        .addOptions(new OptionData(OptionType.STRING, "description", "The description of the rift.").setRequired(true)),
                Commands.slash("leave", "Leaves the rift and removes it from the current channel. (Permission Requirement: Administrator)"),
                Commands.slash("delete-message", "Deletes the supplied message from every channel. (Permission Requirement: Manage Messages)")
                        .addOptions(new OptionData(OptionType.STRING, "message_id", "The ID of the message to delete.").setRequired(true)),
                Commands.slash("join", "Joins an existing rift with the supplied token. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "token", "The rift token.").setRequired(true)),
                Commands.slash("global_modify", "Modifies global channel settings. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "name", "The name of the rift.").setRequired(true))
                        .addOptions(new OptionData(OptionType.STRING, "description", "The description of the rift.").setRequired(true)),
                Commands.slash("modify", "Modifies local channel settings. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "prefix", "The prefix of the channel.").setRequired(false))
                        .addOptions(new OptionData(OptionType.STRING, "description", "The description of the channel.").setRequired(false))
                        .addOptions(new OptionData(OptionType.STRING, "invite_code", "The guild invite code. (not the URL)").setRequired(false)),
                Commands.slash("set_prefix", "Sets the channel prefix. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "prefix", "The prefix of the channel.").setRequired(true)),
                Commands.slash("set_description", "Sets the channel description. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "description", "The description of the channel.").setRequired(true)),
                Commands.slash("set_invite", "Sets the guild invite code. (Permission Requirement: Administrator)")
                        .addOptions(new OptionData(OptionType.STRING, "invite_code", "The guild invite code. (not the URL)").setRequired(true))
        ).queue();
    }

    public static void update_status() {
        jda.getPresence().setActivity(
                Activity.watching(
                        "over " +
                        jda.getGuilds().size() +
                        " guilds"
                )
        );
    }

    public static void main(String[] args) throws IOException {
        configHandler = new ConfigHandler();

        String token = configHandler.config.get("token").getAsString();
        if (token.equals("")) {
            System.out.println("Token not specified in config! Exiting...");
            return;
        }

        try {
            listener = new Listener();
            jda = JDABuilder.createLight(token).addEventListeners(listener).build();
        } catch (LoginException e) {
            System.out.println("Unable to login! Please check the token and try again. Exiting...");
            return;
        }

        riftData = new RiftData();
        webhookHandler = new WebhookHandler();

        valid_prefix_regex = configHandler.config.get("prefix_regex").getAsString();

        JsonArray tmp = configHandler.config.getAsJsonArray("debug_administrators");
        for (int i = 0; i < tmp.size(); i++) {
            debug_administrators.add(tmp.get(i).getAsString());
        }

        if (configHandler.config.get("update_commands").getAsBoolean()) {
            updateCommands();
            configHandler.config.addProperty("update_commands", false);
            configHandler.save();
        }
    }
}