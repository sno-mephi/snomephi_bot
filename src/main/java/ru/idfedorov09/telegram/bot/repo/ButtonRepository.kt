package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.GlobalConstants.MAX_BROADCAST_BUTTONS_COUNT
import ru.idfedorov09.telegram.bot.data.model.Button

interface ButtonRepository : JpaRepository<Button, Long> {

    @Query(
        """
            SELECT *
                FROM button_table
                WHERE author_id = :userId
                ORDER BY last_modify_dttm DESC 
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun getLastModifiedButtonByUserId(userId: Long): Button?

    @Query(
        """
            SELECT *
                FROM button_table
                WHERE 1=1
                    AND button_text IS NOT NULL 
                    AND broadcast_id = :broadcastId
                ORDER BY button_id
            LIMIT ${MAX_BROADCAST_BUTTONS_COUNT + 1}
        """,
        nativeQuery = true
    )
    fun findAllValidButtonsForBroadcast(broadcastId: Long): List<Button>
}