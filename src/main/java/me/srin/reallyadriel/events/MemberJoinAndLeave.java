package me.srin.reallyadriel.events;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.database.Database;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(staticName = "createEvent")
public class MemberJoinAndLeave extends ListenerAdapter {
    private final Database database;
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        database.addMemberIfNotPresent(event.getMember(), event.getGuild());
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        val member = event.getMember();
        if (member == null) {
            return;
        }
        database.removeMember(member, event.getGuild());
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        val guild = event.getGuild();
        guild.getMembers().forEach(member -> database.addMemberIfNotPresent(member, guild));
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        val guild = event.getGuild();
        guild.getMembers().forEach(member -> database.removeMember(member, guild));
    }
}
