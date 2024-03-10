package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.Broadcast

interface BroadcastRepository : JpaRepository<Broadcast, Long> {
    @Query(
        """
            SELECT * 
            FROM broadcast_table
            WHERE 1=1
                AND is_completed IS false
                AND TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP) >= TIMEZONE('Europe/Moscow', broadcast_start_dttm)
                AND is_built IS true
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findFirstActiveBroadcast(): Broadcast?

    @Query(
        """
        SELECT b FROM Broadcast b
        WHERE b.authorId = :authorId AND b.isBuilt = false AND b.isDeleted = false
        ORDER BY b.id DESC
        LIMIT 1
    """,
    )
    fun findLatestUnbuiltBroadcastByAuthor(authorId: Long): Broadcast?

    @Query(
        """
            SELECT * 
            FROM broadcast_table
            WHERE 1=1
                AND is_weekly IS true
                AND TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP) <= TIMEZONE('Europe/Moscow', DATE_ADD(broadcast_start_dttm, INTERVAL 1 WEEK))
                AND is_built IS true 
            ORDER BY broadcast_start_dttm DESC
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findFirstActiveWeeklyBroadcast(): Broadcast?


}
