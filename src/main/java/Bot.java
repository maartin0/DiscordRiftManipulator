import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Bot {
    public static Bot bot;
    public static JDA getJDA() {
        return Bot.bot.jda;
    }
    public JDA jda;
    public Bot(String token, Object... listeners) throws InstantiationException, LoginException {
        if (Bot.bot != null) throw new InstantiationException("Bot already initialized");
        jda = JDABuilder.createLight(token).addEventListeners(listeners).build();
        Bot.bot = this;
    }
}
