package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Таблица с сертификатами
 */
@Entity
@Table(name = "certificates_table")
data class Certificates(
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
)