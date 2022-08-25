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
                                        new SubcommandData("description", "Modify the local channel description")
                                            .addOption(OptionType.STRING, "description", "local channel description", true),
                                        new SubcommandData("invite", "Modify the invite code for the guild (NOT the URL)")
                                                .addOption(OptionType.STRING, "code", "guild invite code", true))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setGuildOnly(true))
                .command(Commands.slash("purge", "Purge x messages globally") // TODO: purge function
                        .addOption(OptionType.NUMBER, "number", "number of messages to purge", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                        .setGuildOnly(true))
                .command(Commands.message("Delete message") // TODO: delete function
                        .setGuildOnly(true))
                .command(Commands.message("Pin message") // TODO: pin function
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
                        .setGuildOnly(true))
                .command(Commands.user("Toggle mute") // TODO: mute function
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                        .setGuildOnly(true))
                .updateCommands();
    }
    static void verboseSave() {
        System.out.println("Saving...");
        try {
            AppConfig.save();
        } catch (IOException e) {
            System.out.println("Warning: Unable to save configuration");
            return;
        }
        System.out.println("Saved");
    }
    static class Listener extends ListenerAdapter {
        @Override
        public void onReady(@NotNull ReadyEvent event) {
            System.out.println("Loading rifts from storage...");
            Rift.loadAll();
            if (AppConfig.updateCommands) {
                System.out.println("Updating global commands...");
                updateCommands();
                AppConfig.updateCommands = false;
            }
            long result = Rift.purgeAll();
            if (result > 0) {
                System.out.println("Purged " + result + " empty rift(s)");
                verboseSave();
            }
            if (AppConfig.autosave)
                Executors.newScheduledThreadPool(1).scheduleAtFixedRate(Main::verboseSave, AppConfig.autosaveInterval, AppConfig.autosaveInterval, TimeUnit.MINUTES);
            System.out.println("Ready!");
        }
        @Override
        public void onShutdown(@NotNull ShutdownEvent event) {
            System.out.println("Saving data...");
            try {
                Rift.saveAll();
            } catch (IOException e) {
                System.out.println("An error occurred while trying to save rift data");
                e.printStackTrace();
            }
            System.out.println("Saving finished");
        }
    }
    public static void main(String[] args) throws IOException, InstantiationException {
        System.out.println("Loading config...");
        AppConfig.load();
        try {
            System.out.println("Initializing bot...");
            Bot.load(
                AppConfig.token,
                new Forwarder.Listener(),
                new ManagementCommandListener(),
                new ModerationCommandListener(),
                new UtilCommandListener(),
                new Listener()
            );
        } catch (LoginException e) {
            System.out.println("Unable to load bot, is the token correct?");
        }
        // TODO: Schedule save all rifts every 5 mins (configurable)
    }
}
