import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RiftData {

    static final String TOKEN_PATH = "data/token_data.json";
    static final String SERVER_PATH = "data/server_data.json";

    static final String ASCII_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final Integer SIMPLE_RECURSE_LIMIT = 20;

    JsonObject tokens;
    JsonObject servers;

    public RiftData() throws IOException {
        load();
    }

    void load() throws IOException {
        if (Files.exists(Path.of(TOKEN_PATH))) {
            tokens = JsonParser.parseString(
                    Files.readString(
                            Path.of(TOKEN_PATH)
                    )
            ).getAsJsonObject();
        }

        if (Files.exists(Path.of(SERVER_PATH))) {
            servers = JsonParser.parseString(
                    Files.readString(
                            Path.of(SERVER_PATH)
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
                Path.of(TOKEN_PATH),
                tokens.toString().getBytes()
        );
        Files.write(
                Path.of(SERVER_PATH),
                servers.toString().getBytes()
        );
    }

    public boolean isInvalidPrefix(String prefix) {
        for (char c : prefix.toCharArray()) {
            if (!String.valueOf(c).matches(Main.valid_prefix_regex)) {
                return true;
            }
        }

        return (prefix.length() > Main.MAX_PREFIX_LENGTH || prefix.length() < 1);
    }

    public String getPrefix(String phrase) {
        StringBuilder result = new StringBuilder();
        for (String word : phrase.split(" ")) {
            if (word.length() == 0) continue;

            String start_char = String.valueOf(word.toUpperCase(Locale.ROOT).charAt(0));
            if (ASCII_UPPERCASE.contains(start_char)) {
                result.append(start_char);
            }
        }

        if (isInvalidPrefix(result.toString())) return "ERR";

        return result.toString();
    }

    public String genToken() {
        return UUID.randomUUID().toString();
    }

    public HashMap<String, List<String>> getRiftChannels(String token) {
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

    public String getRiftName(String token) {
        try {
            return tokens.getAsJsonObject(token).get("name").getAsString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean channelHasRift(String guild_id, String channel_id) {
        try {
            if (servers.keySet().contains(guild_id)) {
                return servers.getAsJsonObject(guild_id).has(channel_id);
            }
        } catch (NullPointerException e) {
            return false;
        }

        return false;
    }

    public String getDescription(String guildID, String channel_id) {
        String token;
        try {
            token = servers.getAsJsonObject(guildID).get(channel_id).getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        String riftName;
        String value;
        try {
            riftName = tokens.getAsJsonObject(token).get("name").getAsString();
            value = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(guildID).get("description").getAsString();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append(riftName);
        result.append("\n\n");
        result.append(value);
        result.append("\n\n");
        result.append("=".repeat(30));
        result.append("\n\n");

        HashMap<String, List<String>> channels = getRiftChannels(token);

        LinkedHashMap<String, String> serverTexts = new LinkedHashMap<>();

        for (String serverID : channels.keySet()) {
            StringBuilder serverText = new StringBuilder();
            String name;
            String prefix;
            String invite;

            try {
                name = Main.jda.getGuildById(serverID).getName();
                JsonObject sobject = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(serverID);
                prefix = sobject.get("prefix").getAsString();
                invite = sobject.get("invite").getAsString();
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }

            serverText.append("[");
            serverText.append(prefix);
            serverText.append("] - ");
            serverText.append(name);

            if (!(invite.equals("") || invite.equals(" "))) {
                serverText.append(": ");
                serverText.append("https://discord.gg/");
                serverText.append(invite);
            }

            serverText.append("\n");

            serverTexts.put(name, serverText.toString());
        }

        serverTexts.keySet().stream().sorted().forEach(a -> result.append(serverTexts.get(a)));

        return result.toString();
    }

    public String createRiftData(String name, String description, String creatorID, String serverID, String channelID) {
        String token = genToken();
        int recursions = 0;
        while (tokens.keySet().contains(token)) {
            if (recursions > SIMPLE_RECURSE_LIMIT) return null;
            token = genToken();
            recursions += 1;
        }

        // Create token config
        JsonObject rift = new JsonObject();
            rift.addProperty("name", name);
            rift.addProperty("description", description);
            rift.addProperty("creator_guild", serverID);

            JsonObject channels = new JsonObject();
                JsonObject serversObj = new JsonObject();
                    serversObj.addProperty("manager_id", creatorID);
                    serversObj.addProperty("prefix", getPrefix(Objects.requireNonNull(Main.jda.getGuildById(serverID)).getName()));
                    serversObj.addProperty("description", description);
                    serversObj.addProperty("invite", " ");

                    JsonArray channelArray = new JsonArray();
                        channelArray.add(channelID);

                    serversObj.add("channels", channelArray);

                channels.add(serverID, serversObj);

            rift.add("channels", channels);

        tokens.add(token, rift);

        // Update server config
        JsonObject serverMapObj;
        if (servers.keySet().contains(serverID)) {
            serverMapObj = servers.getAsJsonObject(serverID);
        } else {
            serverMapObj = new JsonObject();
        }

        serverMapObj.addProperty(channelID, token);

        servers.add(serverID, serverMapObj);

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return token;
    }

    public String addRiftData(String token, String managerID, String serverID, String channelID) {
        if (!tokens.keySet().contains(token)) return null;
        JsonObject channels_object = tokens.getAsJsonObject(token).getAsJsonObject("channels");

        if (!channels_object.keySet().contains(serverID)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("manager_id", managerID);
            obj.addProperty("prefix", getPrefix(Main.jda.getGuildById(serverID).getName()));
            obj.addProperty("description", tokens.getAsJsonObject(token).get("description").getAsString());
            obj.addProperty("invite", " ");
            obj.add("channels", new JsonArray());
            channels_object.add(serverID, obj);
        }

        JsonArray channels = channels_object.getAsJsonObject(serverID).getAsJsonArray("channels");

        if (arrayContains(channels, channelID)) return null;

        channels.add(channelID);

        JsonObject serverMapObj;
        if (servers.keySet().contains(serverID)) {
            serverMapObj = servers.getAsJsonObject(serverID);
        } else {
            serverMapObj = new JsonObject();
        }

        serverMapObj.addProperty(channelID, token);

        if (!servers.keySet().contains(serverID)) servers.add(serverID, serverMapObj);

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens.getAsJsonObject(token).get("description").getAsString();
    }

    boolean arrayContains(JsonArray channels, String channelID) {
        for (JsonElement e : channels) {
            if (e.getAsString().equals(channelID)) return true;
        }
        return false;
    }

    public boolean deleteRiftData(String guildID, String channelID) {
        String token;

        try {
            token = servers.getAsJsonObject(guildID).get(channelID).getAsString();
        } catch (NullPointerException e) {
            return false;
        }

        if (!tokens.keySet().contains(token)) return false;

        JsonObject rift;
        JsonObject server;
        JsonArray channels;
        try {
            rift = tokens.getAsJsonObject(token);
            server = rift.getAsJsonObject("channels").getAsJsonObject(guildID);
            channels = server.getAsJsonArray("channels");
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).getAsString().equals(channelID)) {
                channels.remove(i);
                break;
            }
        }
        if (channels.size() == 0) {
            rift.getAsJsonObject("channels").remove(guildID);
        }

        if (rift.getAsJsonObject("channels").keySet().size() == 0) {
            tokens.remove(token);
        }

        JsonObject server_obj = servers.getAsJsonObject(guildID);

        server_obj.remove(channelID);

        if (server_obj.keySet().size() == 0) {
            servers.remove(guildID);
        }

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public void clearGuildRifts(String guildID) {
        JsonObject guildObj = servers.getAsJsonObject(guildID);
        for (String channelID : guildObj.keySet()) {
            deleteRiftData(guildID, channelID);
        }
    }

    public String getChannelToken(TextChannel channel) {
        String guildID = channel.getGuild().getId();
        String channelID = channel.getId();
        if (!channelHasRift(guildID, channelID)) return null;
        return Main.riftData.servers.getAsJsonObject(guildID).get(channelID).getAsString();
    }

    public boolean modifyRiftData(String serverID, String channelID, String newPrefix, String newDescription, String newInvite) {
        try {
            String token = servers.getAsJsonObject(serverID).get(channelID).getAsString();
            JsonObject server_obj = tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(serverID);
            if (!(newPrefix == null)) server_obj.addProperty("prefix", newPrefix);
            if (!(newDescription == null)) server_obj.addProperty("description", newDescription);
            if (!(newInvite == null)) server_obj.addProperty("invite", newInvite);
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

    public boolean modifyGlobalRiftData(String token, String name, String description) {
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
