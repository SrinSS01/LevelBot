package me.srin.reallyadriel.events;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.Poll;
import me.srin.reallyadriel.database.Database;
import me.srin.reallyadriel.database.EmojiToRole;
import me.srin.reallyadriel.database.Users;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AllArgsConstructor(staticName = "createEvent")
public class SlashCommand extends ListenerAdapter {
    private final Database database;
    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommand.class);
    private static final Pattern CUSTOM_EMOJI_PATTERN = Pattern.compile("<a?:([a-zA-Z0-9_]+):([0-9]+)>");
    private static final Pattern UNICODE_EMOJI_PATTERN = Pattern.compile("\\X", Pattern.UNICODE_CHARACTER_CLASS);
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            var command = event.getName();
            val split = command.split("-");
            val builder = new StringBuilder();
            builder.append(split[0]);
            for (int i = 1; i < split.length; i++) {
                val str = split[i];
                val ch = str.charAt(0);
                builder.append(str.replaceFirst("" + ch, "" + Character.toUpperCase(ch)));
            }
            command = builder.toString();

            getClass().getDeclaredMethod(command, SlashCommandInteractionEvent.class).invoke(this, event);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void say(SlashCommandInteractionEvent event) {
        OptionMapping messageOption = event.getOption("message");
        String message = Objects.requireNonNull(messageOption).getAsString();

        MessageChannel channel;
        OptionMapping channelOption = event.getOption("channel");

        if (channelOption != null) {
            channel = channelOption.getAsChannel().asGuildMessageChannel();
        } else {
            channel = event.getChannel();
        }
        channel.sendMessage(message).queue();
        event.reply("Message sent.").setEphemeral(true).queue();
    }

    private void purge(SlashCommandInteractionEvent event) {
        if (Objects.requireNonNull(event.getMember()).hasPermission(Permission.MESSAGE_MANAGE)) {
            val channel = event.getOption("channel");
            GuildMessageChannel messageChannel;
            if (channel != null) {
                messageChannel = channel.getAsChannel().asGuildMessageChannel();
            } else {
                messageChannel = event.getChannel().asGuildMessageChannel();
            }
            val amount = Objects.requireNonNull(event.getOption("amount")).getAsInt();
            val user = Objects.requireNonNull(event.getOption("user")).getAsUser();
            messageChannel.getHistory()
                    .retrievePast(amount)
                    .queue(messages ->
                            messageChannel
                                    .purgeMessages(messages.stream()
                                            .filter(message -> message.getAuthor().getIdLong() == user.getIdLong())
                                            .collect(Collectors.toList())));
            event.reply("Deleting messages...").setEphemeral(true).queue();
        } else {
            event.reply("You don't have permission to do this.").setEphemeral(true).queue();
        }
    }

    private void rank(SlashCommandInteractionEvent event) {
        val userOptional = event.getOption("user");
        User user;
        if (userOptional == null) {
            user = event.getUser();
        } else user = userOptional.getAsUser();
        val guild = event.getGuild();
        if (guild == null) {
            event.deferReply(true).queue();
            return;
        }
        event.deferReply().queue();
        val hook = event.getHook();
        val usersRepository = database.getUsersRepository();
        val userIdLong = user.getIdLong();
        val guildIdLong = guild.getIdLong();
        val users = usersRepository.findById(Users.ID.of(userIdLong, guildIdLong));
        val rank = usersRepository.getRank(userIdLong, guildIdLong);
        users.ifPresentOrElse(usr -> {
            usr.setRank(rank);
            usr.setPfp(user.getEffectiveAvatarUrl());
            usr.setName(user.getName());
            val statsURL = getStatsURL(usr);
            val image = "Rank%d.png".formatted(System.currentTimeMillis());
            val file = new File(image);
            file.deleteOnExit();
            try (InputStream in = new URL(statsURL).openStream()) {
                Files.copy(in, Paths.get(image));
            } catch (IOException ignore) {}
            hook.editOriginalEmbeds(new EmbedBuilder()
                    .setColor(0x2f3136)
                    .setImage("attachment://" + image)
                    .build()).setAttachments(FileUpload.fromData(file)).queue();
        }, () -> event.deferReply(true).queue());
    }

    private void poll(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        val topic = URLEncoder.encode(Objects.requireNonNull(event.getOption("topic")).getAsString(), StandardCharsets.UTF_8);
        Poll poll = Poll.make(topic);
        int count = 0;
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            val optionalOption = event.getOption("option-" + (i + 1));
            if (optionalOption == null) {
                continue;
            }
            poll.addOption(Poll.Option.create(i + 1, URLEncoder.encode(optionalOption.getAsString(), StandardCharsets.UTF_8)));
            count++;
            buttons.add(Button.primary(String.valueOf(i + 1), String.valueOf(i + 1)));
        }
        val image = "Poll%d.png".formatted(System.currentTimeMillis());
        val file = new File(image);
        file.deleteOnExit();
        try (InputStream in = new URL(poll.toString()).openStream()) {
            Files.copy(in, Paths.get(image));
        } catch (IOException ignore) {}

        event.getHook().editOriginalEmbeds(
                new EmbedBuilder()
                        .setImage("attachment://" + image)
                        .setColor(0x2f3136)
                        .build()
        ).setAttachments(FileUpload.fromData(file)).setComponents(ActionRow.of(buttons))
                .queue(message -> Database.POLL_MAP.put(message.getIdLong(), poll));
    }

    private void sendEmbed(SlashCommandInteractionEvent event) {
        val description = Objects.requireNonNull(event.getOption("description")).getAsString();
        val title = Objects.requireNonNull(event.getOption("title")).getAsString();
        val thumbnail = event.getOption("thumbnail");
        OptionMapping colorOption = event.getOption("color");
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setDescription(description)
                .setTitle(title)
                .setColor(colorOption == null? 0x2f3136: Integer.parseInt(colorOption.getAsString(), 16))
                .setThumbnail(thumbnail == null? null: thumbnail.getAsString())
                .build()).queue();
        event.reply("Sent!").setEphemeral(true).queue();
    }

    private void editEmbed(SlashCommandInteractionEvent event) {
        val messageId = Objects.requireNonNull(event.getOption("message-id")).getAsString();
        val description = Objects.requireNonNull(event.getOption("description")).getAsString();
        val title = Objects.requireNonNull(event.getOption("title")).getAsString();
        val thumbnail = event.getOption("thumbnail");
        OptionMapping colorOption = event.getOption("color");
        event.getChannel().editMessageEmbedsById(messageId, new EmbedBuilder()
                .setDescription(description)
                .setTitle(title)
                .setColor(colorOption == null? 0x2f3136: Integer.parseInt(colorOption.getAsString(), 16))
                .setThumbnail(thumbnail == null? null: thumbnail.getAsString())
                .build()).queue();
        event.reply("Edited!").setEphemeral(true).queue();
    }

    private void addReactionRole(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> hook.deleteOriginal().queue());
        val messageId = Objects.requireNonNull(event.getOption("message-id")).getAsString();
        val buttons = new ArrayList<Button>(4);
        for (int i = 0; i < 10; i++) {
            val index = i + 1;
            val reaction = event.getOption("reaction-" + index);
            val role = event.getOption("role-" + index);
            if (reaction == null) {
                break;
            }
            val reactionAsString = reaction.getAsString();
            if (role == null) {
                event.replyFormat("role not provided for %s", reactionAsString).setEphemeral(true).queue();
                return;
            }
            val custom = CUSTOM_EMOJI_PATTERN.matcher(reactionAsString);
            val unicode = UNICODE_EMOJI_PATTERN.matcher(reactionAsString);
            if (custom.matches()) {
                val emojiUnion = Emoji.fromFormatted(reactionAsString);
                val emojiToRole = new EmojiToRole();
                emojiToRole.setEmojiId(reactionAsString);
                emojiToRole.setMessageId(Long.parseLong(messageId));
                emojiToRole.setRoleId(role.getAsRole().getIdLong());
                database.getEmojiToRoleRepository().save(emojiToRole);
                buttons.add(Button.primary(reactionAsString, emojiUnion));
            } else if (unicode.matches()) {
                val emojiUnion = Emoji.fromFormatted(reactionAsString);
                val emojiToRole = new EmojiToRole();
                val asCodepoints = emojiUnion.asUnicode().getAsCodepoints();
                emojiToRole.setEmojiId(asCodepoints);
                emojiToRole.setMessageId(Long.parseLong(messageId));
                emojiToRole.setRoleId(role.getAsRole().getIdLong());
                database.getEmojiToRoleRepository().save(emojiToRole);
                buttons.add(Button.primary(asCodepoints, emojiUnion));
            } else {
                event.reply("Invalid emoji: " + reactionAsString).setEphemeral(true).queue();
                return;
            }
        }
        event.getChannel().editMessageComponentsById(messageId, ActionRow.of(buttons)).queue();
    }

    // non slash command
    private String getStatsURL(Users users) {
        val config = database.getConfig();
        return "https://user-bot-next-app.vercel.app/api/og?" +
                "xp=" + users.getXp() + '&' +
                "xp-limit=" + users.getXpLimit() + '&' +
//                .append("color=").append("blue").append('&')
                "level=" + users.getLevel() + '&' +
                "rank=" + users.getRank() + '&' +
                "background=" + URLEncoder.encode(config.getRankCardBackgroundUrl(), StandardCharsets.UTF_8) + '&' +
                "name=" + URLEncoder.encode(users.getName(), StandardCharsets.UTF_8) + '&' +
                "profile=" + URLEncoder.encode(users.getPfp(), StandardCharsets.UTF_8);
    }
}
