import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
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
