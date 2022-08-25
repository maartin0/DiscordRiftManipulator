package me.maartin0;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import me.maartin0.util.Bot;
import me.maartin0.util.JsonFile;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Rift {
    public static class RiftGuild {
        public Guild guild;
        public String managerId;
        public String prefix;
        public String description;
        public String invite;
        public RiftGuild(Guild guild, String managerId, String prefix, String description, String invite) {
            this.guild = guild;
            this.managerId = managerId;
            this.prefix = prefix;
            this.description = description;
            this.invite = invite;
        }
        @Override
        public String toString() {
            return "{\nguild: " + guild + ",\n"
            + "managerId: " + managerId + ",\n"
            + "prefix: " + prefix + ",\n"
            + "description: " + description + "\n}";
        }
        public enum WarnReason {
            WEBHOOK,
            SEND_MESSAGE,
            EDIT_MESSAGE,
            DELETE_MESSAGE
        }
        public void warn(WarnReason reason) {
            // TODO
            System.out.println("Warning generated (method not implemented): " + reason);
        }
        public static String generatePrefix(String guildName) {
            return guildName.chars()
                    .mapToObj(c -> (char) c)
                    .filter(Character::isUpperCase)
                    .map(Object::toString)
                    .collect(Collectors.joining());
        }
    }
    public static class RiftChannel {
        public RiftGuild guild;
        public TextChannel channel;
        public RiftChannel(RiftGuild guild, TextChannel channel) {
            this.guild = guild;
            this.channel = channel;
        }
        @Override
        public String toString() {
            return "{\nguild: " + guild + ",\n"
            + "channel: " + channel + "\n}";
        }
        public Webhook getWebhook() throws MissingAccessException {
            return channel.retrieveWebhooks()
                    .complete()
                    .stream()
                    .findFirst()
                    .orElseGet(() -> channel.createWebhook("Rift Handler").complete());
        }
    }
    public String token;
    public String name;
    public String description;
    public String primaryGuildId;
    public Collection<RiftChannel> channels;
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
        rifts.add(this);
    }
    @Override
    public String toString() {
        return "{\ntoken: " + this.token + ",\n"
        + "name: " + this.name + ",\n"
        + "description: " + this.description + ",\n"
        + "primaryGuildId: " + this.primaryGuildId + ",\n"
        + "channels: " + channels + "\n}";
    }
    public Optional<RiftChannel> getRiftChannel(TextChannel channel) {
        return channels.stream().filter((RiftChannel riftChannel) -> riftChannel.channel.getIdLong() == channel.getIdLong()).findFirst();
    }
    public void addChannel(TextChannel channel, User manager, String description) {
        this.channels.add(
                new Rift.RiftChannel(
                        new Rift.RiftGuild(
                                channel.getGuild(),
                                manager.getId(),
                                Rift.RiftGuild.generatePrefix(channel.getGuild().getName()),
                                description,
                                ""
                        ),
                        channel
                )
        );
    }
    public void removeChannel(TextChannel channel) {
        this.channels = this.channels.stream().filter((RiftChannel item) -> !item.channel.getId().equals(channel.getId())).toList();
    }
    public boolean deleteIfEmpty() {
        if (channels.size() == 0) {
            this.delete();
            return true;
        }
        return false;
    }
    public void delete() {
        deleteRift(this);
    }
    static Collection<Rift> rifts = new ArrayList<>();
    static Map<String, Map<String, Rift>> lookup = new ConcurrentHashMap<>();
    static JsonFile tokenData = new JsonFile("data/token_data.json");
    public static Optional<Rift> lookupFromChannel(TextChannel channel) {
        Map<String, Rift> serverObject = lookup.get(channel.getGuild().getId());
        if (serverObject == null) return Optional.empty();
        return Optional.ofNullable(serverObject.get(channel.getId()));
    }
    public static Optional<Rift> fromToken(String token) {
        return rifts.stream().filter((Rift rift) -> rift.token.equals(token)).findFirst();
    }
    private static void deleteRift(Rift rift) {
        rifts = rifts.stream().filter((Rift item) -> !item.token.equals(rift.token)).toList();
    }
    public static void loadAll() {
        tokenData.forceLoad();
        tokenData.data.entrySet().forEach((Map.Entry<String, JsonElement> entry) -> {
            JsonObject object = entry.getValue().getAsJsonObject();
            Collection<RiftChannel> channels = new ArrayList<>();
            object.get("channels").getAsJsonObject().entrySet().forEach((Map.Entry<String, JsonElement> serverEntry) -> {
                Guild guild = Bot.getJDA().getGuildById(serverEntry.getKey());
                if (guild == null) return;
                JsonObject server = serverEntry.getValue().getAsJsonObject();
                RiftGuild riftGuild = new RiftGuild(
                        guild,
                        server.get("manager_id").getAsString(),
                        server.get("prefix").getAsString(),
                        server.get("description").getAsString(),
                        server.get("invite").getAsString()
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
            new Rift(
                    entry.getKey(),
                    object.get("name").getAsString(),
                    object.get("description").getAsString(),
                    object.get("creator_guild").getAsString(),
                    channels
            );
        });
    }
    public static long purgeAll() {
        return rifts.stream().map(Rift::deleteIfEmpty).filter(Boolean::booleanValue).count();
    }
    private static JsonObject serialize() {
        JsonObject result = new JsonObject();
        rifts.forEach((Rift rift) -> {
            JsonObject riftObject = new JsonObject();
            riftObject.addProperty("name", rift.name);
            riftObject.addProperty("description", rift.description);
            riftObject.addProperty("creator_guild", rift.primaryGuildId);
            JsonObject serversObject = new JsonObject();
            rift.channels.forEach((RiftChannel channel) -> {
                JsonElement storedElement = serversObject.get(channel.guild.guild.getId());
                JsonObject serverObject;
                JsonArray channelsArray;
                if (storedElement == null) {
                    serverObject = new JsonObject();
                    serverObject.addProperty("manager_id", channel.guild.managerId);
                    serverObject.addProperty("prefix", channel.guild.prefix);
                    serverObject.addProperty("description", channel.guild.description);
                    serverObject.addProperty("invite", channel.guild.invite);
                    channelsArray = new JsonArray();
                } else {
                    serverObject = storedElement.getAsJsonObject();
                    channelsArray = serverObject.get("channels").getAsJsonArray();
                }
                channelsArray.add(channel.channel.getId());
                serverObject.add("channels", channelsArray);
                serversObject.add(channel.guild.guild.getId(), serverObject);
            });
            riftObject.add("channels", serversObject);
            result.add(rift.token, riftObject);
        });
        return result;
    }
    public static void saveAll() throws IOException {
        tokenData.data = Rift.serialize();
        tokenData.save();
    }
}
