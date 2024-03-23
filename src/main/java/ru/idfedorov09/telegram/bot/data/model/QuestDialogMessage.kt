package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "question_dialog_messages_table")
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
    @Column(name = "message_photo_hash", columnDefinition = "TEXT")
    val messagePhotoHash: String? = null,
    @Column(name = "message_document_hash", columnDefinition = "TEXT")
    val messageDocumentHash: String? = null,
    @Column(name = "sticker_hash", columnDefinition = "TEXT")
    val stickerHash: String? = null,
    @Column(name = "voice_hash", columnDefinition = "TEXT")
    val voiceHash: String? = null,
    @Column(name = "video_hash", columnDefinition = "TEXT")
    val videoHash: String? = null,
    @Column(name = "video_note_hash", columnDefinition = "TEXT")
    val videoNoteHash: String? = null,
    @Column(name = "audio_hash", columnDefinition = "TEXT")
    val audioHash: String? = null,
    @Column(name = "message_id")
    val messageId: Int? = null,
    @Column(name = "message_dttm")
    val messageTime: LocalDateTime? = null,
)
