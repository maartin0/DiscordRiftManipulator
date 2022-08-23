import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Collection;

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
    String description;
    String primaryGuildId;
    Collection<RiftChannel> channels;
    public Rift(String token, String description, String primaryGuildId, Collection<RiftChannel> channels) {
        this.token = token;
        this.description = description;
        this.primaryGuildId = primaryGuildId;
        this.channels = channels;
    }
}
