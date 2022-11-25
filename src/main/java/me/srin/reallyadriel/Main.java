package me.srin.reallyadriel;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.database.Database;
import me.srin.reallyadriel.events.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

@AllArgsConstructor
@Component
public class Main implements CommandLineRunner {
    final Database database;
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public void run(String... args) {
        val token = database.getConfig().getToken();
        val scanner = new Scanner(System.in);
        LOGGER.info("Started bot with token: {}", token);
        val jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GUILD_PRESENCES,
                        GUILD_MEMBERS,
                        GUILD_MESSAGES,
                        GUILD_EMOJIS_AND_STICKERS,
                        GUILD_VOICE_STATES,
                        MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.CLIENT_STATUS)
                .disableCache(
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.VOICE_STATE
                )
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(
                        MemberJoinAndLeave.createEvent(database),
                        GuildReady.createEvent(database),
                        SlashCommand.createEvent(database),
                        MessageSend.createEvent(database),
                        ButtonPress.createEvent(database)
                ).build();
        Utils.EXECUTOR.scheduleWithFixedDelay(() -> {
            if (scanner.nextLine().equals("stop")) {
                Utils.EXECUTOR.shutdownNow();
                jda.shutdownNow();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
