package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ban_table")
data class Ban(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ban_id")
    val id: Long? = null,
    /** причина бана **/
    @Column(name = "reason_text", columnDefinition = "TEXT")
    val text: String? = null,
    /** тот кто забанил **/
    @Column(name = "moderator_id")
    val moderatorId: Long? = null,
    /** tui того кого забанили **/
    @Column(name = "user_tui", columnDefinition = "TEXT")
    val userTui: String? = null,
    /** время начала бана **/
    @Column(name = "start_dttm")
    val startTime: LocalDateTime? = null,
    /** время окончания бана **/
    @Column(name = "finish_dttm")
    val finishTime: LocalDateTime? = null,
    /** флаг подтверждения бана **/
    @Column(name = "is_built")
    val isBuilt: Boolean = false,
    /** true только в случае если юзер отменяет выдачу бана **/
    @Column(name = "is_deleted")
    val isDeleted: Boolean = false,
    /** флаг разбана **/
    @Column(name = "is_unban")
    val isUnban: Boolean = false,
    /** нужно для редактирования консоли при создании бана **/
    @Column(name = "last_console_message_id")
    val lastConsoleMessageId: Int? = null,
    /** id диалога в котором нажата кнопка бан **/
    @Column(name = "quest_dialog_id")
    val questDialogId: Long? = null,
)
