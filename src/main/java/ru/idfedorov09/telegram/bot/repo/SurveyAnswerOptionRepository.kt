package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.idfedorov09.telegram.bot.data.model.SurveyAnswerOption
import ru.idfedorov09.telegram.bot.data.model.SurveyQuestion

interface SurveyAnswerOptionRepository : JpaRepository<SurveyAnswerOption, Long> {
    @Query(
        """
            SELECT * 
            FROM survey_answers_option_table
            WHERE 1=1
                AND survey_question_id = :surveyQuestionId
                AND is_deleted = false
        """,
        nativeQuery = true,
    )
    fun findAllSurveyAnswerOptionByQuestion(surveyQuestionId: Long) : List<SurveyAnswerOption>

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE survey_answers_option_table
            SET is_deleted = true
            WHERE 1=1
                AND survey_question_id = :surveyQuestionId
        """,
        nativeQuery = true,
    )
    fun deleteAnswerOptionByQuestion(surveyQuestionId: Long)
}