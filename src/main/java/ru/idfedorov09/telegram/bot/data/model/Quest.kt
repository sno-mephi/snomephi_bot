package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus

@Entity
@Table(name = "questions_table")
data class Quest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "author_id")
    val authorId: Long? = null,

    @Column(name = "responder_id")
    val responderId: Long? = null,

    /** айдишники элементов диалога/вопросов/ответов **/
    @Column(name = "dialog_history", columnDefinition = "TEXT")
    val dialogHistory: MutableList<Long>? = null,

    @Column(name = "question_status")
    val questionStatus: QuestionStatus = QuestionStatus.WAIT,
)
