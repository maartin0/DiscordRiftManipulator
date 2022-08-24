package util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

public class Bot {
    public static Bot bot;
    public static JDA getJDA() {
        return bot.jda;
    }
    public JDA jda;
    private Bot(JDA jda) {
        this.jda = jda;
    }
    public static void load(String token, Object... listeners) throws InstantiationException, LoginException {
        if (bot != null) throw new InstantiationException("util.Bot already initialized");
        bot = new Bot(
                JDABuilder.createLight(token)
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(listeners)
                        .build()
        );
    }
    private List<CommandData> commandCache = new ArrayList<>();
    public Bot command(CommandData command) {
        commandCache.add(command);
        return this;
    }
    public void updateCommands() {
        jda.updateCommands().addCommands(commandCache).queue();
        commandCache = new ArrayList<>();
    }
}
