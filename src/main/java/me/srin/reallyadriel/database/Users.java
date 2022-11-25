package me.srin.reallyadriel.database;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Consumer;

@Getter
@Setter
@Entity
@Table(name = "users")
@IdClass(Users.ID.class)
public class Users {
    @Id private long userId;
    @Id private long guildId;
    int xp;
    int level;

    @Column(columnDefinition = "int default 300")
    int xpLimit = 300;

    int totalXp;

    long coolDown;

    @Transient
    int rank = 0;
    @Transient
    String pfp;
    @Transient
    String name;
    @Transient
    private static final Random RANDOM = new Random();
    @Transient
    private static final int LEVEL_CONSTANT = 3;

    private static int getRandomXp() {
        return RANDOM.nextInt(1, 10);
    }

    public void gainXp(Consumer<Integer> onLevelUp) {
        xp += getRandomXp();
        if (xp >= xpLimit) {
            System.out.println(level);
            ++level;
            System.out.println(level);
            xp = xp - xpLimit;
            xpLimit = (level + 1) * (level + 1) * LEVEL_CONSTANT * 100;
            System.out.println(xpLimit);
            onLevelUp.accept(level);
        }
    }

    public void loseXp(Consumer<Integer> onLevelDown) {
        xp -= getRandomXp();
        if (xp <= 0) {
            --level;
            if (level < 0) {
                level = 0;
                xp = 0;
                return;
            }
            int previousXpLimit = (level - 1) * (level - 1) * LEVEL_CONSTANT * 100;
            xp = previousXpLimit + xp;
            xpLimit = previousXpLimit;
            onLevelDown.accept(level + 1);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    @EqualsAndHashCode
    public static class ID implements Serializable {
        private long userId, guildId;
    }
}
