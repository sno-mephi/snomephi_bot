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
                AND CURRENT_TIMESTAMP >= broadcast_start_dttm AT TIME ZONE 'Europe/Moscow'
                AND is_built IS true
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findFirstActiveBroadcast(): Broadcast?
}
