package me.srin.reallyadriel.events;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.database.Database;
import me.srin.reallyadriel.database.EmojiToRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@AllArgsConstructor(staticName = "createEvent")
public class ButtonPress extends ListenerAdapter {
    private final Database database;
//    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonPress.class);

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        val emoji = event.getButton().getEmoji();
        val messageIdLong = event.getMessageIdLong();
        if (emoji != null) {
            val member = Objects.requireNonNull(event.getMember());
            val guild = Objects.requireNonNull(event.getGuild());
            val format = switch (emoji.getType()) {
                case CUSTOM -> emoji.asCustom().getAsMention();
                case UNICODE -> emoji.asUnicode().getAsCodepoints();
            };
            val byId = database.getEmojiToRoleRepository().findById(EmojiToRole.ID.of(format, messageIdLong));
            if (byId.isEmpty()) {
                event.deferEdit().queue();
                return;
            }
            val roleById = Objects.requireNonNull(guild.getRoleById(byId.get().getRoleId()));
            member.getRoles().stream().filter(role -> role.equals(roleById)).findAny().ifPresentOrElse(
                    role -> {
                        guild.removeRoleFromMember(member, role).queue();
                        event.replyFormat("role removed %s", role.getAsMention()).setEphemeral(true).queue();
                    },
                    () -> {
                        guild.addRoleToMember(member, roleById).queue();
                        event.replyFormat("role added %s", roleById.getAsMention()).setEphemeral(true).queue();
                    }
            );
            return;
        }
        event.deferEdit().queue();
        val buttonId = Integer.parseInt(Objects.requireNonNull(event.getButton().getId()));
        val poll = Database.POLL_MAP.get(messageIdLong);
        if (poll == null) {
            return;
        }
        poll.incrementOption(buttonId);

        val image = "Poll%d.png".formatted(System.currentTimeMillis());
        val file = new File(image);
        file.deleteOnExit();
        try (InputStream in = new URL(poll.toString()).openStream()) {
            Files.copy(in, Paths.get(image));
        } catch (IOException ignore) {}

        event.getHook().editOriginalEmbeds(new EmbedBuilder()
                .setColor(0x2f3136)
                .setImage("attachment://" + image)
                .build()).setAttachments(FileUpload.fromData(file)).queue();
    }
}
