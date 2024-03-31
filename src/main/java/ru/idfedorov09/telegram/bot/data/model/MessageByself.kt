package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.idfedorov09.telegram.bot.data.enums.SentMessageStatus
import ru.idfedorov09.telegram.bot.data.model.converter.BotMessageTypeConverter
import ru.idfedorov09.telegram.bot.data.model.converter.SentMessageStatusTypeConverter

/**
 * Таблица с отправленными сообщениями (не аналитическая: данные об отправленном сообщении хранятся в виде json)
 */
@Entity
@Table(name = "messages_byself")
data class MessageByself (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    val id: Long? = null,

    /**
     * Столбец с MessageParams отправляемого сообщения. В бд хранится в виде json
     */
    @Column(name = "message_object", columnDefinition = "TEXT")
    @Convert(converter = BotMessageTypeConverter::class)
    val messageParams: MessageParams? = null,

    /** Статус по отправке этого сообщения **/
    @Column(name = "status", columnDefinition = "TEXT")
    @Convert(converter = SentMessageStatusTypeConverter::class)
    val status: SentMessageStatus = SentMessageStatus.DEFAULT_STATUS
)