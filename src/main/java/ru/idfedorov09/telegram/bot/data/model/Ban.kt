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
    @Column(name = "moderator_id", columnDefinition = "TEXT")
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
    /** флаг разбана **/
    @Column(name = "")
    val isUnban: Boolean = false
)
