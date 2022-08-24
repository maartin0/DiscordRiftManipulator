package old;

import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHandler {
    static final String filepath = "config.json";
    JsonObject config;
    public ConfigHandler() throws IOException {
        reload();
    }
    void reload() throws IOException {
        Path path = Path.of(filepath);
        if (Files.exists(path)) {
            config = JsonParser.parseString(
                Files.readString(path)
            ).getAsJsonObject();
        }

        if (config == null || config.keySet().size() == 0) {
            System.out.println("Config not found; Creating new file.");
            config = new JsonObject();
            config.addProperty("token", "");
            config.addProperty("prefix_regex", "[A-Za-zÀ-ÖØ-öø-ÿ0-9\\-_]");
            config.add("debug_administrators", new JsonArray());
            config.addProperty("update_commands", true);
            save();
        }
    }
    public void save() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(
                Path.of(filepath),
                gson.toJson(config).getBytes()
        );
    }
}