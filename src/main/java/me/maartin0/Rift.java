package me.maartin0;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.maartin0.util.AppConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import me.maartin0.util.Bot;
import me.maartin0.util.JsonFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
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
            WEBHOOK("fetching webhook(s)"),
            SEND_MESSAGE("sending message(s)"),
            EDIT_MESSAGE("editing message(s)"),
            DELETE_MESSAGE("deleting message(s)");
            final String when;
            WarnReason(String when) {
                this.when = when;
            }
        }
        public void warn(WarnReason reason, @Nullable String errorMessage) {
            StringBuilder message = new StringBuilder();
            message.append("An error occurred when ").append(reason.when).append(".");
            if (errorMessage != null) message.append(":\n").append(errorMessage);
            if (AppConfig.debug) Main.log(message);
            Member guildOwner = guild.getOwner();
            if (guildOwner == null) return;
            guildOwner.getUser()
                    .openPrivateChannel()
                    .complete()
                    .sendMessage(message.toString())
                    .queue();
        }
        public static String generatePrefix(String guildName) {
            return guildName.chars()
                    .mapToObj(c -> (char) c)
                    .filter(Character::isUpperCase)
                    .map(Object::toString)
                    .collect(Collectors.joining());
        }
        public String getDisplayString() {
            StringBuilder result = new StringBuilder();
            result.append("[").append(prefix).append("]").append(" - ").append(guild.getName());
            if (invite.length() > 0) result.append(": https://discord.gg/").append(invite);
            return result.toString();
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
    public Collection<String> mutes;
    public boolean isMuted(User user) {
        return mutes.contains(user.getId());
    }
    public void mute(User user) {
        if (!isMuted(user)) {
            mutes.add(user.getId());
        }
    }
    public void unmute(User user) {
        if (isMuted(user)) {
            mutes.remove(user.getId());
        }
    }
    public Rift(String token, String name, String description, String primaryGuildId, Collection<RiftChannel> channels, Collection<String> mutes) {
        this.token = token;
        this.name = name;
        this.description = description;
        this.primaryGuildId = primaryGuildId;
        this.channels = channels;
        this.mutes = mutes;
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
    public String getInfo() {
        StringBuilder result = new StringBuilder();
        result.append(this.name).append("\n\n")
                .append(this.description).append("\n\n")
                .append("=".repeat(30)).append("\n\n");
        channels.stream()
                .map((RiftChannel channel) -> channel.guild)
                .distinct()
                .map(RiftGuild::getDisplayString)
                .forEach((String line) -> result.append(line).append("\n"));
        return result.toString();
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
                        server.get("invite") == null ? "" : server.get("invite").getAsString()
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
                    channels,
                    object.get("mutes") == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(new Gson().fromJson(object.getAsJsonArray("mutes"), String[].class)))
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
            JsonArray mutes = new JsonArray();
            rift.mutes.forEach(mutes::add);
            riftObject.add("mutes", mutes);
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
        tokenData.save(AppConfig.debug);
    }
    public static void reloadAll() throws IOException {
        saveAll();
        loadAll();
    }
}
