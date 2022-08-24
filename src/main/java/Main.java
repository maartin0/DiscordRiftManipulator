import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InstantiationException {
        AppConfig.load();

        try {
            Bot.load(AppConfig.token);
        } catch (LoginException e) {
            System.out.println("Unable to load bot, is the token correct?");
        }

        Rift.loadAll();

        System.out.println(Rift.rifts);
    }
}
