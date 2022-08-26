package me.maartin0.util;

import com.google.gson.JsonObject;
import java.io.IOException;

public class AppConfig {
    static JsonFile config;
    public static String token;
    public static String prefixRegex;
    public static boolean updateCommands;
    public static boolean autosave;
    public static int autosaveInterval;
    public static boolean debug;
    public static boolean quiet;
    static void generate() {
        config.data = new JsonObject();
        config.data.addProperty("token", "");
        config.data.addProperty("prefix_regex_comment", "The regex used to validate prefixes");
        config.data.addProperty("prefix_regex", "[A-Za-zÀ-ÖØ-öø-ÿ0-9\\-_]");
        config.data.addProperty("update_commands_comment", "Set this value to true to force an update of global commands on the next restart, this property will automatically be set to false afterwards.");
        config.data.addProperty("update_commands", true);
        config.data.addProperty("autosave", true);
        config.data.addProperty("autosave_interval_minutes", 5);
        config.data.addProperty("debug_mode_comment", "Do not use ephemeral messages between interactions, show warnings in console, pretty print data json file(s)");
        config.data.addProperty("debug_mode", false);
        config.data.addProperty("quiet_comment", "Do not log messages using System.out.println");
        config.data.addProperty("quiet_mode", false);
    }
    public static void load() throws IOException {
        config = new JsonFile("config.json");
        try {
            config.load();
        } catch (IOException e) {
            System.out.println("Config not found; Creating new file.");
            generate();
            config.save(true);
        }
        token = config.getString("token");
        prefixRegex = config.getString("prefix_regex");
        updateCommands = config.data.get("update_commands").getAsBoolean();
        autosave = config.data.get("autosave").getAsBoolean();
        autosaveInterval = config.data.get("autosave_interval_minutes").getAsInt();
        debug = config.data.get("debug_mode").getAsBoolean();
        quiet = config.data.get("quiet_mode").getAsBoolean();
    }
    public static void save() throws IOException {
        config.data.addProperty("update_commands", updateCommands);
        config.save(true);
    }
}
