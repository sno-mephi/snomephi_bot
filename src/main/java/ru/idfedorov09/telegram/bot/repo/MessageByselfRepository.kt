package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.enums.SentMessageStatus
import ru.idfedorov09.telegram.bot.data.model.MessageByself

interface MessageByselfRepository : JpaRepository<MessageByself, Long> {

    @Query(
        value = """
            SELECT msg FROM MessageByself msg
            WHERE msg.status = :status
            ORDER BY msg.id
        """,
    )
    fun findAllMessagesByStatus(status: SentMessageStatus): List<MessageByself>

}