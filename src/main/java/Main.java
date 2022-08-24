import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import util.AppConfig;
import util.Bot;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    static void updateCommands() {
        Bot.bot
                .command(Commands.slash("create", "Create a new rift")
                        .addOption(OptionType.STRING, "name", "rift name", true)
                        .addOption(OptionType.STRING, "description", "rift description", true))
                .command(Commands.slash("leave", "Remove the rift from the current channel"))
                .command(Commands.slash("join", "Join an existing rift")
                        .addOption(OptionType.STRING, "token", "rift token", true))
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
                                                .addOption(OptionType.STRING, "invite", "guild invite code")))
                .command(Commands.slash("purge", "Purge x messages globally")
                        .addOption(OptionType.NUMBER, "number", "number of messages to purge", true))
                .updateCommands();
    }
    public static void main(String[] args) throws IOException, InstantiationException {
        AppConfig.load();

        try {
            Bot.load(AppConfig.token, new ListenerAdapter() {
                @Override
                public void onReady(@NotNull ReadyEvent event) {
                    Rift.loadAll();
                }
            }, new Forwarder.Listener());
        } catch (LoginException e) {
            System.out.println("Unable to load bot, is the token correct?");
        }
    }
}
