package me.maartin0;

import me.maartin0.interactions.ManagementCommandListener;
import me.maartin0.interactions.ModerationCommandListener;
import me.maartin0.interactions.UtilCommandListener;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import me.maartin0.util.AppConfig;
import me.maartin0.util.Bot;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    static void updateCommands() {
        Bot.bot
                .command(Commands.slash("create", "Create a new rift")
                        .addOption(OptionType.STRING, "name", "rift name", true)
                        .addOption(OptionType.STRING, "description", "rift description", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setGuildOnly(true))
                .command(Commands.slash("leave", "Remove the rift from the current channel")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setGuildOnly(true))
                .command(Commands.slash("join", "Join an existing rift")
                        .addOption(OptionType.STRING, "token", "rift token", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setGuildOnly(true))
                .command(Commands.slash("modify", "Modify rift settings")
                        .addSubcommandGroups(new SubcommandGroupData("global", "Modify global rift settings")
                                .addSubcommands(new SubcommandData("name", "Modify the global rift name")
                                                    .addOption(OptionType.STRING, "name", "global rift name", true),
                                                new SubcommandData("description", "Modify the global rift description")
                                                    .addOption(OptionType.STRING, "description", "global rift description", true)))
                        .addSubcommands(new SubcommandData("prefix", "Modify the guild's prefix")
                                            .addOption(OptionType.STRING, "prefix", "guild prefix", true),
                                        new SubcommandData("invite", "Modify the invite code for the guild (NOT the URL)")
                                                .addOption(OptionType.STRING, "code", "guild invite code", true))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setGuildOnly(true))
                .command(Commands.slash("purge", "Purge x messages globally")
                        .addOption(OptionType.INTEGER, "number", "number of messages to purge", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                        .setGuildOnly(true))
                .command(Commands.slash("info", "Get information about the current rift")
                        .setGuildOnly(true))
                .command(Commands.slash("description", "Effectively pastes the output of /info into the channel description")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)))
                .command(Commands.message("Delete message")
                        .setGuildOnly(true))
                .command(Commands.message("Pin message")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                        .setGuildOnly(true))
                .command(Commands.user("Toggle mute")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                        .setGuildOnly(true))
                .updateCommands();
    }
    public static void save() {
        Main.log("Saving...");
        try {
            Rift.saveAll();
        } catch (IOException e) {
            Main.log("Warning: Unable to save rift data");
            return;
        }
        Main.log("Saved");
    }
    public static void log(Object message) {
        log(message, false);
    }
    public static void log(Object message, boolean important) {
        if (important || !AppConfig.quiet) {
            System.out.println(message);
        }
    }
    static class Listener extends ListenerAdapter {
        @Override
        public void onReady(@NotNull ReadyEvent event) {
            Main.log("Loading rifts from storage...");
            Rift.loadAll();
            if (AppConfig.updateCommands) {
                Main.log("Updating global commands...");
                updateCommands();
                AppConfig.updateCommands = false;
                try {
                    AppConfig.save();
                } catch (IOException e) {
                    Main.log("Warning: Unable to save app config");
                    return;
                }
            }
            long result = Rift.purgeAll();
            if (result > 0) {
                Main.log("Purged " + result + " empty rift(s)");
            }
            save();
            if (AppConfig.autosave)
                Executors.newScheduledThreadPool(1).scheduleAtFixedRate(Main::save, AppConfig.autosaveInterval, AppConfig.autosaveInterval, TimeUnit.MINUTES);
            Main.log("Ready!", true);
        }
        @Override
        public void onShutdown(@NotNull ShutdownEvent event) {
            Main.log("Saving data...");
            try {
                Rift.saveAll();
            } catch (IOException e) {
                Main.log("An error occurred while trying to save rift data");
                e.printStackTrace();
            }
            Main.log("Saving finished");
        }
    }
    public static void main(String[] args) throws IOException, InstantiationException {
        Main.log("Loading config...", true);
        AppConfig.load();
        try {
            Main.log("Initializing bot...");
            Bot.load(
                AppConfig.token,
                new Forwarder.Listener(),
                new ManagementCommandListener(),
                new ModerationCommandListener(),
                new UtilCommandListener(),
                new Listener()
            );
        } catch (LoginException e) {
            Main.log("Unable to load bot, is the token correct?", true);
        }
    }
}
