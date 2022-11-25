package me.srin.reallyadriel.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import me.srin.reallyadriel.Config;
import me.srin.reallyadriel.Poll;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
@AllArgsConstructor
@Getter
public class Database {
    private final EmojiToRoleRepository emojiToRoleRepository;
    private final UsersRepository usersRepository;
    private final Config config;
    public static final Map<Long /*message id*/, Poll> POLL_MAP = new HashMap<>();
    public static final Map<Long /*user id*/, ScheduledFuture<?>> USER_XP_COOLDOWN_CACHE = new HashMap<>();

    public void addMemberIfNotPresent(Member member, Guild guild) {
        if (member.getUser().isBot()) return;
        val guildIdLong = guild.getIdLong();
        val level0RoleId = config.getLevelRoleMap().get("0");
        if (level0RoleId != null) {
            val level0Role = guild.getRoleById(level0RoleId);
            if (level0Role != null) {
                guild.addRoleToMember(member, level0Role).queue();
            }
        }
        val idLong = member.getIdLong();
        val usersRepository = getUsersRepository();
        val optionalUsers = usersRepository.findById(Users.ID.of(idLong, guildIdLong));
        if (optionalUsers.isEmpty()) {
            val users = new Users();
            users.setUserId(idLong);
            users.setGuildId(guildIdLong);

            usersRepository.save(users);
        }
    }
    public void removeMember(Member member, Guild guild) {
        if (member.getUser().isBot()) return;
        val guildIdLong = guild.getIdLong();
        val idLong = member.getIdLong();
        val usersRepository = getUsersRepository();
        val optionalUsers = usersRepository.findById(Users.ID.of(idLong, guildIdLong));
        optionalUsers.ifPresent(usersRepository::delete);
    }
}
