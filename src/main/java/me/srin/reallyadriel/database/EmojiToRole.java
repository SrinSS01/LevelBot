package me.srin.reallyadriel.database;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "emoji_to_role")
@Getter @Setter
@IdClass(EmojiToRole.ID.class)
public class EmojiToRole {
    @Id
    private String emojiId;
    @Id
    private Long messageId;
    private long roleId;

    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    @Getter @Setter
    @EqualsAndHashCode
    public static class ID implements Serializable {
        private String emojiId;
        private Long messageId;
    }
}