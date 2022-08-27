package me.maartin0;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class Searcher {
    public static class Result {
        Collection<Message> messages;
        public Result(Collection<Message> messages) {
            this.messages = messages;
        }
        public Collection<Message> get() {
            return this.messages;
        }
        public Result exclude(Message message) {
            return new Result(
                    this.messages
                            .stream()
                            .filter((Message item) -> !item.getId().equals(message.getId()))
                            .toList()
            );
        }
    }
    Rift rift;
    Message origin;
    boolean real;
    boolean valid = false;
    String realUsername;
    String copyContent;
    String copyUsername;
    public Searcher(Message origin) {
        Optional<Rift> optionalRift = Rift.lookupFromChannel(origin.getChannel().asTextChannel());
        if (optionalRift.isEmpty()) return;
        this.rift = optionalRift.get();
        Optional<Rift.RiftChannel> optionalRiftChannel = this.rift.getRiftChannel(origin.getChannel().asTextChannel());
        if (optionalRiftChannel.isEmpty()) return;
        Rift.RiftChannel riftChannel = optionalRiftChannel.get();
        this.real = origin.getAuthor().isBot() || origin.getAuthor().isSystem();
        this.origin = origin;
        if (this.real) {
            copyContent = Forwarder.getWebhookMessageContent(origin);
            copyUsername = Forwarder.getWebhookUsername(riftChannel.guild.prefix, origin.getAuthor().getName());
        } else {
            realUsername = origin.getAuthor().getName();
        }
        valid = true;
    }
    boolean matches(Message other) {
        if (this.real) return other.getAuthor().getName().equals(copyUsername) && other.getContentRaw().equals(copyContent);
        else return other.getAuthor().getName().contains(realUsername);
    }
    public Result search() {
        if (!valid) return new Result(new ArrayList<>());
        return new Result(
                this.rift.channels.stream()
                        .map(this::find)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList()
        );
    }
    Optional<Message> find(Rift.RiftChannel channel) {
        return channel.channel.getHistory()
                .retrievePast(50)
                .complete()
                .stream()
                .filter(this::matches)
                .findFirst();
    }
}
