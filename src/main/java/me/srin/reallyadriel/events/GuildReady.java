package me.srin.reallyadriel.events;

import lombok.AllArgsConstructor;
import lombok.val;
import me.srin.reallyadriel.database.Database;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(staticName = "createEvent")
public class GuildReady extends ListenerAdapter {
    private final Database database;
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        // Say
        OptionData s1 = new OptionData(OptionType.STRING, "message", "Say something", true);
        OptionData s2 = new OptionData(OptionType.CHANNEL, "channel", "Say something").setChannelTypes(ChannelType.TEXT);
        commandData.add(Commands.slash("say", "Say something").addOptions(s1, s2));

        // Purge
        OptionData p2 = new OptionData(OptionType.INTEGER, "amount","How much?", true).setRequiredRange(1, 100);
        OptionData userOption = new OptionData(OptionType.USER, "user", "The user who's messages are to be purged", true);
        OptionData p1 = new OptionData(OptionType.CHANNEL, "channel", "Where to delete?").setChannelTypes(ChannelType.TEXT);
        commandData.add(Commands.slash("purge", "The Purge").addOptions(p2, userOption, p1));

        // Poll
        commandData.add(Commands.slash("poll", "Create a poll").addOptions(
                new OptionData(OptionType.STRING, "topic", "Description of the poll", true),
                new OptionData(OptionType.STRING, "option-1", "1st option", true),
                new OptionData(OptionType.STRING, "option-2", "2nd option", true),
                new OptionData(OptionType.STRING, "option-3", "3rd option"),
                new OptionData(OptionType.STRING, "option-4", "4th option")
        ));

        // Send embed message
        commandData.add(Commands.slash("send-embed", "Send an embed message").addOptions(
                new OptionData(OptionType.STRING, "description", "Description", true),
                new OptionData(OptionType.STRING, "title", "Title", true),
                new OptionData(OptionType.STRING, "thumbnail", "url for the thumbnail of the embed"),
                new OptionData(OptionType.STRING, "color", "hex code of the color")
        ));

        // Edit embed message
        commandData.add(Commands.slash("edit-embed", "Edit an embed message").addOptions(
                new OptionData(OptionType.STRING, "message-id", "Message ID of the embed", true),
                new OptionData(OptionType.STRING, "description", "Description", true),
                new OptionData(OptionType.STRING, "title", "Title", true),
                new OptionData(OptionType.STRING, "thumbnail", "url for the thumbnail of the embed"),
                new OptionData(OptionType.STRING, "color", "hex code of the color")
        ));

        // Reaction role
        val options = new ArrayList<OptionData>(10);
        options.add(new OptionData(OptionType.STRING, "message-id", "Message ID of the embed", true));
        for (int i = 0; i < 10; i++) {
            val index = i + 1;
            options.add(new OptionData(OptionType.STRING, "reaction-" + index, "The reaction button texts for each reactions", i < 2));
            options.add(new OptionData(OptionType.ROLE, "role-" + index, "The corresponding role for reaction-" + index, i < 2));
        }
        commandData.add(Commands.slash("add-reaction-role", "creates a reaction role message")
                .addOptions(options));

        // Rank check
        commandData.add(Commands.slash("rank", "Check rank")
                .addOptions(new OptionData(OptionType.USER, "user", "User who's rank you want to check")));

        // Command Data Update
        val guild = event.getGuild();
        guild.updateCommands().addCommands(commandData).queue();

        guild.getMembers().forEach(member -> database.addMemberIfNotPresent(member, guild));
    }
}
