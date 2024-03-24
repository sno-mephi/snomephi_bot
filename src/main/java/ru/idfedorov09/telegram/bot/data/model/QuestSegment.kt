package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "question_segments_table")
data class QuestSegment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_segment_id")
    val id: Long? = null,
    @Column(name = "quest_dialog_id")
    val questId: Long? = null,
    @Column(name = "responder_id")
    val responderId: Long? = null,
    @Column(name = "start_dttm")
    val startTime: LocalDateTime? = null,
    @Column(name = "finish_dttm")
    val finishTime: LocalDateTime? = null,
)