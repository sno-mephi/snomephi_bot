package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.Ban
import java.sql.Struct

interface BanRepository : JpaRepository<Ban, Long> {
    @Query(
        """
            SELECT
                CASE WHEN COUNT(*) = 0 THEN false ELSE true END
            FROM ban_table
            WHERE 1=1
                AND is_unban IS false
                AND user_tui = :tui
        """, nativeQuery = true
    )
    fun isBanned(tui: String): Boolean

    @Query(
        """
            UPDATE ban_table
            SET is_unban = true
            WHERE TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP) > TIMEZONE('Europe/Moscow', finish_dttm) 
        """, nativeQuery = true
    )
    fun updateBanInfo()
}