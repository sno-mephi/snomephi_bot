package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.model.converter.QuestionStatusConverter
import java.time.LocalDateTime

@Entity
@Table(name = "question_dialogs_table")
data class QuestDialog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_dialog_id")
    val id: Long? = null,
    @Column(name = "author_id")
    val authorId: Long? = null,
    /** id сообщения-пульта чата респондентов **/
    @Column(name = "last_quest_segment_id")
    val lastQuestSegmentId: Long? = null,
    @Column(name = "console_message_id")
    val consoleMessageId: String? = null,
    /** айдишники элементов диалога/вопросов/ответов **/
    @Column(name = "dialog_history")
    val dialogHistory: MutableList<Long> = mutableListOf(),
    @Column(name = "start_dttm")
    val startTime: LocalDateTime? = null,
    @Column(name = "finish_dttm")
    val finishTime: LocalDateTime? = null,
    @Column(name = "question_status", columnDefinition = "TEXT")
    @Convert(converter = QuestionStatusConverter::class)
    val questionStatus: QuestionStatus = QuestionStatus.WAIT,
)
