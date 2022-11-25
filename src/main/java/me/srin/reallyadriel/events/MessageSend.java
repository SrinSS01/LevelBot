package me.srin.reallyadriel.events;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.Utils;
import me.srin.reallyadriel.database.Database;
import me.srin.reallyadriel.database.Users;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor(staticName = "createEvent")
public class MessageSend extends ListenerAdapter {
    private final Database database;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSend.class);
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        val author = event.getAuthor();
        if (author.isBot()) return;
        val currentTime = System.currentTimeMillis() / 1000;
        val config = database.getConfig();
        val xpCoolDownInSeconds = config.getXpCoolDownInSeconds();
        val deLevelingCountdownInSeconds = config.getDeLevelingCountdownInSeconds();
        val userIdLong = author.getIdLong();
        val usersRepository = database.getUsersRepository();
        val guild = event.getGuild();
        val optionalUser = usersRepository.findById(Users.ID.of(userIdLong, guild.getIdLong()));
        if (optionalUser.isEmpty()) {
            return;
        }
        val user = optionalUser.get();
        val scheduledFuture = Database.USER_XP_COOLDOWN_CACHE.get(userIdLong);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        if (currentTime - user.getCoolDown() <= xpCoolDownInSeconds) {
            return;
        }

        user.setCoolDown(currentTime);
        user.gainXp(/*on level up*/ level -> {
            val roleId = config.getLevelRoleMap().get(String.valueOf(level));
            if (roleId == null) {
                return;
            }
            val roleById = guild.getRoleById(roleId);
            if (roleById == null) {
                LOGGER.error("invalid role id: {}", roleId);
                return;
            }
            guild.addRoleToMember(author, roleById).queue();
        });
        usersRepository.save(user);

        Database.USER_XP_COOLDOWN_CACHE.put(userIdLong,
            Utils.EXECUTOR.scheduleWithFixedDelay(
                () -> {
                    if (user.getLevel() == 0 && user.getXp() == 0) {
                        return;
                    }
                    user.loseXp(/*on level down*/ level -> {
                        val roleId = config.getLevelRoleMap().get(String.valueOf(level));
                        if (roleId == null) {
                            return;
                        }
                        val roleById = guild.getRoleById(roleId);
                        if (roleById == null) {
                            LOGGER.error("invalid role id: {}", roleId);
                            return;
                        }
                        guild.removeRoleFromMember(author, roleById).queue();
                    });
                    usersRepository.save(user);
                },
                deLevelingCountdownInSeconds,
                deLevelingCountdownInSeconds, TimeUnit.SECONDS
            )
        );
        if (!config.getMediaOnlyChannels().contains(event.getChannel().getId())) {
            return;
        }
        val message = event.getMessage();
        val attachments = message.getAttachments().stream()
                .filter(attachment -> {
                    val fileExtension = attachment.getFileExtension();
                    return fileExtension != null && fileExtension.matches("png|jpeg|jpg");
                }).toList();
        if (!message.getContentRaw().matches("^https?://.+\\.(png|jpeg|jpg)$") && attachments.isEmpty()) {
            message.delete().queue();
        }
    }
}
