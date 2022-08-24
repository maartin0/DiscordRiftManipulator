package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class AppConfig {
    static JsonFile config;
    public static String token;
    public static String prefixRegex;
    public static List<String> debugAdministrators;
    public static boolean updateCommands;
    public static List<User> getDebugAdministrators() {
        return debugAdministrators.stream()
                .map(Bot.getJDA()::retrieveUserById)
                .map(RestAction::complete)
                .toList();
    }
    static void generate() {
        config.data = new JsonObject();
        config.data.addProperty("token", "");
        config.data.addProperty("prefix_regex", "[A-Za-zÀ-ÖØ-öø-ÿ0-9\\-_]");
        config.data.add("debug_administrators", new JsonArray());
        config.data.addProperty("update_commands", true);
    }
    public static void load() throws IOException {
        config = new JsonFile("config.json");
        try {
            config.load();
        } catch (IOException e) {
            System.out.println("Config not found; Creating new file.");
            generate();
            config.save();
        }
        token = config.getString("token");
        prefixRegex = config.getString("prefix_regex");
        debugAdministrators = Stream.of(config.data.getAsJsonArray("debug_administrators"))
                .map(JsonElement::getAsString)
                .toList();
        updateCommands = config.data.get("update_commands").getAsBoolean();
    }
    public static void save() throws IOException {
        config.data.addProperty("update_commands", updateCommands);
        config.save(true);
    }
}
