import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Rift {
    public static class RiftGuild {
        public Guild guild;
        public String managerId;
        public String prefix;
        public String description;
        public RiftGuild(Guild guild, String managerId, String prefix, String description) {
            this.guild = guild;
            this.managerId = managerId;
            this.prefix = prefix;
            this.description = description;
        }
    }
    public static class RiftChannel {
        public RiftGuild guild;
        public TextChannel channel;
        public RiftChannel(RiftGuild guild, TextChannel channel) {
            this.guild = guild;
            this.channel = channel;
        }
    }
    String token;
    String name;
    String description;
    String primaryGuildId;
    Collection<RiftChannel> channels;
    public Rift(String token, String name, String description, String primaryGuildId, Collection<RiftChannel> channels) {
        this.token = token;
        this.name = name;
        this.description = description;
        this.primaryGuildId = primaryGuildId;
        this.channels = channels;
        // Populate lookup map
        this.channels.forEach((RiftChannel channel) -> {
            Map<String, Rift> stored = lookup.get(channel.guild.guild.getId());
            Map<String, Rift> map = stored == null ? new ConcurrentHashMap<>() : stored;
            map.put(channel.channel.getId(), this);
            lookup.put(channel.guild.guild.getId(), map);
        });
    }
    static Collection<Rift> rifts = new ArrayList<>();
    static Map<String, Map<String, Rift>> lookup = new ConcurrentHashMap<>();
    static JsonFile tokenData = new JsonFile("data/token_data.json");
    static JsonFile serverData = new JsonFile("data/server_data.json");
    public static void load(JDA jda) {
        tokenData.forceLoad();
        serverData.forceLoad();
        tokenData.data.entrySet().forEach((Map.Entry<String, JsonElement> entry) -> {
            JsonObject object = entry.getValue().getAsJsonObject();
            Collection<RiftChannel> channels = new ArrayList<>();
            object.get("channels").getAsJsonObject().entrySet().forEach((Map.Entry<String, JsonElement> serverEntry) -> {
                Guild guild = jda.getGuildById(serverEntry.getKey());
                if (guild == null) return;
                JsonObject server = serverEntry.getValue().getAsJsonObject();
                RiftGuild riftGuild = new RiftGuild(
                        guild,
                        server.get("manager_id").getAsString(),
                        server.get("prefix").getAsString(),
                        server.get("description").getAsString()
                );
                server.get("channels").getAsJsonArray().forEach((JsonElement jsonElement) -> {
                    TextChannel channel = guild.getTextChannelById(jsonElement.getAsString());
                    if (channel == null) return;
                    channels.add(
                            new RiftChannel(
                                    riftGuild,
                                    channel
                            )
                    );
                });
            });
            rifts.add(new Rift(
                    entry.getKey(),
                    object.get("name").getAsString(),
                    object.get("description").getAsString(),
                    object.get("creator_guild").getAsString(),
                    channels
            ));
        });
    }
    private static JsonObject serialize() {
        JsonObject result = new JsonObject();
        rifts.forEach((Rift rift) -> {
            JsonObject riftObject = new JsonObject();
            riftObject.addProperty("name", rift.name);
            riftObject.addProperty("description", rift.description);
            riftObject.addProperty("creator_guild", rift.primaryGuildId);
            JsonObject channelsObject = new JsonObject();
            // TODO: Serialize guilds individually
            result.add(rift.token, riftObject);
        });
        return result;
    }
}
