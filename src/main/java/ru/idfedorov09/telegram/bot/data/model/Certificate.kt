package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Таблица с сертификатами
 */
@Entity
@Table(name = "certificates_table")
data class Certificate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    val id: Long? = null,

    /**
     * ФИО человека кому должен придти (в теории) сертификат
     */
    @Column(name = "full_name")
    val fullName: String? = null,

    /**
     * Айди челика который залил файл для отправки сертификата
     */
    @Column(name = "issue_author_id")
    val issueAuthorId: Long? = null,

    /**
     * Айди челика которому должен принадлежать сертификат
     * если null - значит такой не нашелся
     */
    @Column(name = "certificate_owner_id")
    val certificateOwnerId: Long? = null,

    /**
     * Айди кандидата на владение сертификатом
     */
    @Column(name = "candidate_owner_id")
    val candidateOwnerId: Long? = null,

    /**
     * айди (хэш) файла сертификата в тг
     */
    @Column(name = "file_id", columnDefinition = "TEXT")
    val certificateHash: String? = null,

    /**
     * Время начала опроса. null, если все хорошо
     */
    @Column(name = "poll_start_dttm")
    val pollStartTime: LocalDateTime? = null,

    /**
     * id сообщения с вопросом о корректности ФИО
     */
    @Column(name = "poll_message_id")
    val pollMessageId: Int? = null,
)