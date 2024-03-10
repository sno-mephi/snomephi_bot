package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Таблица для обработки коллбэков
 */
@Entity
@Table(name = "callback_data_table")
data class CallbackData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    /** id сообщения кнопки **/
    @Column(name = "msg_id")
    val messageId: String? = null,
    /** информация, хранящаяся в коллбеке **/
    @Column(name = "callback_data", columnDefinition = "TEXT")
    val callbackData: String? = null,
    val metaText: String? = null,
)
