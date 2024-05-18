package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.model.Button
import ru.idfedorov09.telegram.bot.data.model.QuestDialog

interface QuestDialogRepository : JpaRepository<QuestDialog, Long>{
    @Query(
        """
            SELECT console_message_id
                FROM question_dialogs_table
                WHERE 1=1
                    AND author_id = :id
             
        """,
        nativeQuery = true,
    )
    fun findAllMessageOfDeletedUser(id: Long?): List<String?>
}
