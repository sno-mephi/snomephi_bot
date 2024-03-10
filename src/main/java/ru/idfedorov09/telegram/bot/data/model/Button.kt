package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "button_table")
data class Button(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "button_id")
    val id: Long? = null,

    /** название на кнопке **/
    @Column(name = "button_text", columnDefinition = "TEXT")
    val text: String? = null,

    /** ссылка в кнопке **/
    @Column(name = "button_link", columnDefinition = "TEXT")
    val link: String? = null,

    @Column(name = "button_callback_data", columnDefinition = "TEXT")
    val callbackData: String? = null,

    @Column(name = "author_id")
    val authorId: Long? = null,

    @Column(name = "last_modify_dttm")
    val lastModifyTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")),

    /** id брадкаста к которому привязана кнопка **/
    @Column(name = "broadcast_id")
    val broadcastId: Long? = null
)