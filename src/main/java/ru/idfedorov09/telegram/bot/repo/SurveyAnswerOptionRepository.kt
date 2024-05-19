package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.SurveyAnswerOption
import ru.idfedorov09.telegram.bot.data.model.SurveyQuestion

interface SurveyAnswerOptionRepository : JpaRepository<SurveyAnswerOption, Long> {
    @Query(
        """
            SELECT * 
            FROM survey_answers_option_table
            WHERE 1=1
                AND survey_question_id = :surveyQuestionId
        """,
        nativeQuery = true,
    )
    fun findAllSurveyAnswerOptionByQuestion(surveyQuestionId: Long) : List<SurveyAnswerOption>
}