package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.idfedorov09.telegram.bot.data.model.Ban

interface BanRepository : JpaRepository<Ban, Long> {
    @Query(
        """
            SELECT
                CASE WHEN COUNT(*) = 0 THEN false ELSE true END
            FROM ban_table
            WHERE 1=1
                AND is_unban IS false
                AND is_built IS true
                AND is_deleted IS false
                AND user_tui = :tui
        """, nativeQuery = true
    )
    fun isBanned(tui: String): Boolean?

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE ban_table
            SET is_unban = true
            WHERE 1=1
                AND TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP) > TIMEZONE('Europe/Moscow', finish_dttm) 
                AND is_built IS true
                AND is_deleted IS false
        """, nativeQuery = true
    )
    fun updateBanInfo()
    @Query(
        """
            SELECT *
            FROM ban_table
            WHERE 1=1
                AND moderator_id = :moderatorId
                AND is_built = false
                AND is_deleted = false
            ORDER BY ban_id DESC
            LIMIT 1
        """, nativeQuery = true
    )
    fun findLatestUnbuiltBanByModerator(moderatorId: Long): Ban?

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE ban_table
            SET 
                is_unban = true,
                finish_dttm = TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP)
            WHERE 1=1
                AND is_unban IS false
                AND is_built IS true
                AND is_deleted IS false
                AND user_tui = :tui
        """, nativeQuery = true
    )
    fun unbanUser(tui: String)
}