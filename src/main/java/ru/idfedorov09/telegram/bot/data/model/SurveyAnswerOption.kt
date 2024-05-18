package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

@Entity
@Table(name = "survey_answers_table")
data class SurveyAnswerOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_answer_option_id")
    val id: Long? = null,
    @Column(name = "survey_question_id")
    val surveyQuestionId: Long? = null,
    /** id рассылки которая является опросом, так же ее уникальный идентификатор **/
    @Column(name = "broadcast_id")
    val broadcastId: Long? = null,
    @Column(name = "option_text", columnDefinition = "TEXT")
    val optionText: String? = null,
    @Column(name = "next_survey_question_id")
    val nextSurveyQuestionId: Long? = null,
    /** Завершает ли опрос этот вариант ответа **/
    @Column(name = "is_finish_answer_option")
    val isFinishAnswerOption: Boolean = false,
)
