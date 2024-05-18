package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

@Entity
@Table(name = "survey_questions_table")
data class SurveyQuestion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_question_id")
    val id: Long? = null,
    /** id рассылки которая является опросом, так же ее уникальный идентификатор **/
    @Column(name = "broadcast_id")
    val broadcastId: Long? = null,
    /** текст вопроса **/
    @Column(name = "answer_text", columnDefinition = "TEXT")
    val text: String? = null,
    @Column(name = "is_first_question")
    val isFirstQuestion: Boolean? = null,
    @Column(name = "is_last_question")
    val isLastQuestion: Boolean? = null,
    @Column(name = "survey_depth")
    val surveyDepth: Long? = null,
    @Column(name = "is_multiply_choice_question")
    val isMultiplyChoiceQuestion: Boolean? = null,
    @Column(name = "is_built")
    val isBuilt: Boolean = false,
    @Column(name = "is_deleted")
    val isDeleted: Boolean = false,
)
