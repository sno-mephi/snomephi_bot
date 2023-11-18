package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

@Entity
@Table(name = "questions_table")
data class QuestDialogMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "quest_id")
    val questId: Long? = null,

    @Column(name = "is_by_quest_author")
    val isByQuestionAuthor: Boolean? = null,

    @Column(name = "author_id")
    val authorId: Long? = null,

    @Column(name = "message_text", columnDefinition = "TEXT")
    val messageText: String? = null,
)
