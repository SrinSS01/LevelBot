package me.srin.reallyadriel.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsersRepository extends JpaRepository<Users, Users.ID> {
    @Query(value = """
        select u.`rank`
        from
            (select usr.user_id, usr.guild_id, rank() over (order by usr.total_xp desc) `rank` from users usr) u
        where u.user_id=?1 and u.guild_id=?2
    """, nativeQuery = true)
    int getRank(long userId, long guildId);
}