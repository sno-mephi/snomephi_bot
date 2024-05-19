package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.SurveyQuestion

interface SurveyQuestionRepository : JpaRepository<SurveyQuestion, Long> {
    @Query(
        """
            SELECT sq.*
            FROM survey_questions_table AS sq
                JOIN broadcast_table AS br
                ON 1=1
                    AND sq.broadcast_id = br.broadcast_id
            WHERE 1=1
                AND br.broadcast_author_id = :authorId 
                AND sq.is_built = false 
                AND sq.is_deleted = false
            ORDER BY sq.survey_question_id DESC
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findLatestUnbuiltSurveyQuestionByAuthor(authorId: Long): SurveyQuestion?

    @Query(
        """
            SELECT *
            FROM survey_questions_table
            WHERE 1=1
                AND broadcast_id = :broadcastId
                AND is_built = true 
                AND is_deleted = false
        """,
        nativeQuery = true
    )
    fun findAllSurveyQuestionByBroadcast(broadcastId: Long): List<SurveyQuestion>

    @Query(
        """
            SELECT *
            FROM survey_questions_table
            WHERE 1=1
                AND broadcast_id = :broadcastId
                AND is_built = true 
                AND is_deleted = false
                AND is_first_question = true
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findFirstQuestionByBroadcast(broadcastId: Long): SurveyQuestion?
}