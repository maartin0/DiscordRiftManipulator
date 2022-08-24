package old;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class MessageFormatter {

    static String replaceMentions(Message message) {
        Map<String, String> toReplace = new HashMap<>();
        message.getMentions().getUsers().forEach((User user) -> {
            String replacement = getNameID(user);
            toReplace.put(String.format("<@%s>", user.getId()), replacement);
            toReplace.put(String.format("<@!%s>", user.getId()), replacement);
        });
        return StringUtils
                .replaceEach(
                        message.getContentRaw(),
                        toReplace.keySet().toArray(String[]::new),
                        toReplace.values().toArray(String[]::new)
                ).replace("@", "@ ");
    }

    public static String getPrefix(TextChannel channel) {
        String token = Main.riftData.getChannelToken(channel);
        if (token == null) return "INF";
        return Main.riftData.tokens.getAsJsonObject(token).getAsJsonObject("channels").getAsJsonObject(channel.getGuild().getId()).get("prefix").getAsString();
    }

    public static String getPrefixedName(TextChannel channel, Member member) {
        return String.format("%s: %s", getPrefix(channel), member.getUser().getName());
    }

    public static String getNameID(User user) {
        return String.format("@ %s#%s", user.getName(), user.getDiscriminator());
    }

    static String getReplyContent(@Nullable Message reply) {
        if (reply == null) return "";
        List<String> content = new ArrayList<>();
        Arrays.stream(reply.getContentRaw().split("\n")).filter(s -> !s.startsWith("> ")).forEach(content::add);
        reply.getAttachments().forEach((Message.Attachment a) -> content.add(String.format("<%s>", a.getUrl())));
        content.add(MessageFormatter.getNameID(reply.getAuthor()));
        return String.format("> %s", String.join("\n> ", content));
    }

    public static String getMessageContent(Message message) {
        return String.join(
                "\n",
                Stream.of(getReplyContent(message.getReferencedMessage()),
                        replaceMentions(message),
                        String.join("\n",message.getStickers().stream().map(StickerItem::getIconUrl).toList()),
                        String.join("\n",message.getAttachments().stream().map(Message.Attachment::getUrl).toList())
                ).filter((String s) -> !(s.isBlank() || s.isEmpty())).toList());
    }

}
