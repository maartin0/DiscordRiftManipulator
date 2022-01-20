import net.dv8tion.jda.api.entities.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class MessageLocator {

    public static List<TextChannel> getRiftChannels(String token, @Nullable TextChannel exclude) {
        HashMap<String, List<String>> channels = Main.riftData.getRiftChannels(token);

        List<TextChannel> result = new ArrayList<>();
        channels.keySet()
                .stream()
                .map(Main.jda::getGuildById)
                .filter(Objects::nonNull)
                .forEach(
                        (Guild guild) -> channels.get(guild.getId())
                                .stream()
                                .map(guild::getTextChannelById)
                                .filter(Objects::nonNull)
                                .filter((TextChannel channel) -> Objects.isNull(exclude) || !channel.getId().equals(exclude.getId()))
                                .forEach(result::add)
                );
        return result;
    }

    public static Message getMessageFromChannel(Predicate<Message> filter, TextChannel channel) {
        MessageHistory history = channel.getHistory();
        for (int i = 0; i < (Main.BACKWARD_SEARCH_MAX / Main.BACKWARD_SEARCH_SKIP); i++) {
            Optional<Message> foundMessage = history
                    .retrievePast(Main.BACKWARD_SEARCH_SKIP)
                    .complete()
                    .stream()
                    .filter(filter)
                    .findFirst();

            if (foundMessage.isEmpty()) continue;

            return foundMessage.get();
        }
        return null;
    }

    public static List<Message> getAllMessages(Message origin) {
        return getAllMessages(origin, false);
    }

    public static List<Message> getAllMessages(Message origin, boolean ignoreContent) {
        TextChannel originChannel = origin.getTextChannel();
        String token = Main.riftData.getChannelToken(originChannel);
        if (Objects.isNull(token) || !origin.isFromGuild() || origin.getAuthor().isSystem()) return Collections.emptyList();

        String plainName;
        String prefixedName;
        String originContent;
        String bodyContent;

        User user = origin.getAuthor();

        if (origin.isWebhookMessage()) {
            prefixedName = user.getName();
            plainName = prefixedName.split(" ")[1];
            bodyContent = origin.getContentRaw();

            List<String> content = new ArrayList<>(Arrays.asList(bodyContent.split("\n")));

            for (String sub : content) {
                if (sub.startsWith("> ")) {
                    content.remove(sub);
                } else {
                    break;
                }
            }

            Collections.reverse(content);

            for (String sub : content) {
                if (sub.startsWith("https://media.discordapp.net/attachments/")) {
                    content.remove(sub);
                } else {
                    break;
                }
            }

            originContent = String.join("\n", content);
        } else {
            Member member = origin.getGuild().retrieveMember(user).complete();
            if (Objects.isNull(member)) return Collections.emptyList();

            plainName = user.getName();
            prefixedName = MessageFormatter.getPrefixedName(originChannel, member);
            originContent = origin.getContentRaw();
            bodyContent = MessageFormatter.getMessageContent(origin);
        }

        List<TextChannel> channels = getRiftChannels(token, null);

        return channels.stream()
                .map((TextChannel channel) -> getMessageFromChannel((Message message) -> (
                        (message.getAuthor().isBot()
                        && message.getAuthor().getName().equals(prefixedName)
                        && (message.getContentRaw().equals(bodyContent) || ignoreContent))
                        || (message.getAuthor().getName().equals(plainName)
                        && (message.getContentRaw().contains(originContent) || ignoreContent))), channel))
                .filter(Objects::nonNull)
                .toList();
    }

}
