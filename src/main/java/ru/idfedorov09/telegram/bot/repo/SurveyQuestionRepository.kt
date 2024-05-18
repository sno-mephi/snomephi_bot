package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.SurveyQuestion

interface SurveyQuestionRepository : JpaRepository<SurveyQuestion, Long> {
    @Query(
        """
            SELECT *
            FROM survey_table
            WHERE 1=1
                AND author_id = :authorId 
                AND is_built = false 
                AND is_deleted = false
            ORDER BY survey_id DESC
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findLatestUnbuiltSurveyQuestionByAuthor(authorId: Long): SurveyQuestion?
}