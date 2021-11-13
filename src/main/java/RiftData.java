import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RiftData {

    static final String token_path = "data/token_data.json";
    static final String server_path = "data/server_data.json";

    static final String ascii_uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final Integer simple_recurse_limit = 20;

    JsonObject tokens;
    JsonObject servers;

    public RiftData() throws IOException {
        reload();
    }

    void reload() throws IOException {
        if (Files.exists(Path.of(token_path))) {
            tokens = JsonParser.parseString(
                    Files.readString(
                            Path.of(token_path)
                    )
            ).getAsJsonObject();
        }

        if (Files.exists(Path.of(server_path))) {
            servers = JsonParser.parseString(
                    Files.readString(
                            Path.of(server_path)
                    )
            ).getAsJsonObject();
        }

        if (tokens == null || servers == null) {
            System.out.println("Data file not found; Creating new file.");

            tokens = new JsonObject();
            servers = new JsonObject();

            save();
        }
    }

    public void save() throws IOException {
        Files.write(
                Path.of(token_path),
                tokens.toString().getBytes()
        );
        Files.write(
                Path.of(server_path),
                servers.toString().getBytes()
        );
    }

    public boolean is_invalid_prefix(String prefix) {
        for (char c : prefix.toCharArray()) {
            if (!String.valueOf(c).matches(Main.valid_prefix_regex)) {
                return true;
            }
        }

        return (prefix.length() > Main.max_prefix_length || prefix.length() < 1);
    }

    public String get_prefix(String phrase) {
        StringBuilder result = new StringBuilder();
        for (String word : phrase.split(" ")) {
            if (word.length() == 0) continue;

            String start_char = String.valueOf(word.toUpperCase(Locale.ROOT).charAt(0));
            if (ascii_uppercase.contains(start_char)) {
                result.append(start_char);
            }
        }

        if (is_invalid_prefix(result.toString())) return "ERR";

        return result.toString();
    }

    public String gen_token() {
        return UUID.randomUUID().toString();
    }

    public HashMap<String, List<String>> get_rift_channels(String token) {
        JsonObject channels;
        try {
            channels = tokens.getAsJsonObject(token).getAsJsonObject("channels");
        } catch (NullPointerException ignored) { return null; }

        HashMap<String, List<String>> result = new HashMap<>();
        for (String server_id : channels.keySet()) {
            ArrayList<String> channel_ids = new ArrayList<>();
            JsonArray channel_arr = channels.getAsJsonObject(server_id).getAsJsonArray("channels");
            for (int i = 0; i < channel_arr.size(); i++) {
                channel_ids.add(channel_arr.get(i).getAsString());
            }
            result.put(server_id, channel_ids);
        }

        return result;
    }

    public String get_rift_name(String token) {
        try {
            return tokens.getAsJsonObject(token).get("name").getAsString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean channel_has_rift(String guild_id, String channel_id) {
        try {
            if (servers.keySet().contains(guild_id)) {
                return servers.getAsJsonObject(guild_id).has(channel_id);
            }
        } catch (NullPointerException e) {
            return false;
        }

        return false;
    }

    @Nullable public String get_description(String guild_id, String channel_id) {
        String token;
        try {
            token = servers.getAsJsonObject(guild_id).get(channel_id).getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        String mname;
        String value;
        try {
            mname = tokens.getAsJsonObject(token).get("name").getAsString();
            value = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(guild_id).get("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append(mname);
        result.append("\n\n");
        result.append(value);
        result.append("\n\n");
        result.append("=".repeat(30));
        result.append("\n\n");

        HashMap<String, List<String>> channels = get_rift_channels(token);

        LinkedHashMap<String, String> server_texts = new LinkedHashMap<>();

        for (String server_id : channels.keySet()) {
            StringBuilder server_text = new StringBuilder();
            String name;
            String prefix;
            String invite;

            try {
                name = Main.jda.getGuildById(server_id).getName();
                JsonObject sobject = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(server_id);
                prefix = sobject.get("prefix").getAsString();
                invite = sobject.get("invite").getAsString();
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }

            server_text.append("[");
            server_text.append(prefix);
            server_text.append("] - ");
            server_text.append(name);

            if (!(invite.equals("") || invite.equals(" "))) {
                server_text.append(": ");
                server_text.append("https://discord.gg/");
                server_text.append(invite);
            }

            server_text.append("\n");

            server_texts.put(name, server_text.toString());
        }

        server_texts.keySet().stream().sorted().forEach(a -> result.append(server_texts.get(a)));

        return result.toString();
    }

    public String create_rift_data(String name, String description, String creator_id, String server_id, String channel_id) {
        String token = gen_token();
        int recursions = 0;
        while (tokens.keySet().contains(token)) {
            if (recursions > simple_recurse_limit) return null;
            token = gen_token();
            recursions += 1;
        }

        // Create token config
        JsonObject rift = new JsonObject();
            rift.addProperty("name", name);
            rift.addProperty("description", description);
            rift.addProperty("creator_guild", server_id);

            JsonObject channels = new JsonObject();
                JsonObject servers_obj = new JsonObject();
                    servers_obj.addProperty("manager_id", creator_id);
                    servers_obj.addProperty("prefix", get_prefix(Objects.requireNonNull(Main.jda.getGuildById(server_id)).getName()));
                    servers_obj.addProperty("description", description);
                    servers_obj.addProperty("invite", " ");

                    JsonArray channel_array = new JsonArray();
                        channel_array.add(channel_id);

                    servers_obj.add("channels", channel_array);

                channels.add(server_id, servers_obj);

            rift.add("channels", channels);

        tokens.add(token, rift);

        // Update server config
        JsonObject server_map_obj;
        if (servers.keySet().contains(server_id)) {
            server_map_obj = servers.getAsJsonObject(server_id);
        } else {
            server_map_obj = new JsonObject();
        }

        server_map_obj.addProperty(channel_id, token);

        servers.add(server_id, server_map_obj);

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return token;
    }

    public String add_rift_data(String token, String manager_id, String server_id, String channel_id) {
        if (!tokens.keySet().contains(token)) return null;
        JsonObject channels_object = tokens.getAsJsonObject(token).getAsJsonObject("channels");

        if (!channels_object.keySet().contains(server_id)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("manager_id", manager_id);
            obj.addProperty("prefix", get_prefix(Main.jda.getGuildById(server_id).getName()));
            obj.addProperty("description", tokens.getAsJsonObject(token).get("description").getAsString());
            obj.addProperty("invite", " ");
            obj.add("channels", new JsonArray());
            channels_object.add(server_id, obj);
        }

        JsonArray channels = channels_object.getAsJsonObject(server_id).getAsJsonArray("channels");

        if (array_contains(channels, channel_id)) return null;

        channels.add(channel_id);

        JsonObject server_map_obj;
        if (servers.keySet().contains(server_id)) {
            server_map_obj = servers.getAsJsonObject(server_id);
        } else {
            server_map_obj = new JsonObject();
        }

        server_map_obj.addProperty(channel_id, token);

        if (!servers.keySet().contains(server_id)) servers.add(server_id, server_map_obj);

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens.getAsJsonObject(token).get("description").getAsString();
    }

    boolean array_contains(JsonArray channels, String channel_id) {
        for (JsonElement e : channels) {
            if (e.getAsString().equals(channel_id)) return true;
        }
        return false;
    }

    public boolean delete_rift_data(String guild_id, String channel_id) {
        String token;

        try {
            token = servers.getAsJsonObject(guild_id).get(channel_id).getAsString();
        } catch (NullPointerException e) {
            return false;
        }

        if (!tokens.keySet().contains(token)) return false;

        JsonObject rift;
        JsonObject server;
        JsonArray channels;
        try {
            rift = tokens.getAsJsonObject(token);
            server = rift.getAsJsonObject("channels").getAsJsonObject(guild_id);
            channels = server.getAsJsonArray("channels");
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getAsString().equals(channel_id)) {
                channels.remove(i);
                break;
            }
        }
        if (channels.size() == 0) {
            rift.getAsJsonObject("channels").remove(guild_id);
        }

        if (rift.getAsJsonObject("channels").keySet().size() == 0) {
            tokens.remove(token);
        }

        JsonObject server_obj = servers.getAsJsonObject(guild_id);

        server_obj.remove(channel_id);

        if (server_obj.keySet().size() == 0) {
            servers.remove(guild_id);
        }

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public void clear_guild_rifts(String guild_id) {
        JsonObject guild_obj = servers.getAsJsonObject(guild_id);
        for (String channel_id : guild_obj.keySet()) {
            delete_rift_data(guild_id, channel_id);
        }
    }

    public boolean modify_rift_data(String server_id, String channel_id, String new_prefix, String new_description, String new_invite) {
        try {
            String token = servers.getAsJsonObject(server_id).get(channel_id).getAsString();
            JsonObject server_obj = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(server_id);
            if (!(new_prefix == null)) server_obj.addProperty("prefix", new_prefix);
            if (!(new_description == null)) server_obj.addProperty("description", new_description);
            if (!(new_invite == null))server_obj.addProperty("invite", new_invite);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean modify_global_rift_data(String token, String name, String description) {
        try {
            JsonObject token_obj = tokens.getAsJsonObject(token);
            token_obj.addProperty("name", name);
            token_obj.addProperty("description", description);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
