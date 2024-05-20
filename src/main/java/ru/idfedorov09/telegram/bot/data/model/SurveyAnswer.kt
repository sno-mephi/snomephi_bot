package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "survey_answers_table")
data class SurveyAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_answer_id")
    val id: Long? = null,
    /** id рассылки которая является опросом, так же ее уникальный идентификатор **/
    @Column(name = "broadcast_id")
    val broadcastId: Long? = null,
    @Column(name = "user_id")
    val userId: Long? = null,
    @Column(name = "survey_question_id")
    val surveyQuestionId: Long? = null,
    @Column(name = "answer", columnDefinition = "TEXT")
    val answer: String? = null,
    @Column(name = "answer_dttm")
    val answerTime: LocalDateTime? = null,
)
