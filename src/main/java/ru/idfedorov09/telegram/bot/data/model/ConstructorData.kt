package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "costructors_table")
data class ConstructorData(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "author_tui")
    val authorTui: String? = null,

    /** id сообщения с консолью **/
    /** если после него было много сообщений, нужно опустить вниз **/
    @Column(name = "last_console_id")
    val lastConsoleId: String? = null,

    /** текст рассылки **/
    @Column(name = "broadcast_text", columnDefinition = "TEXT")
    val text: String? = null,

    @Column(name = "photo_id")
    /** фото рассылки (хэш) **/
    val photoId: String? = null,

    /** дата и время рассылки (по мск) **/
    @Column(name = "broadcast_time")
    val broadcastTime: LocalDateTime? = null,

    // TODO: сохраняем кнопки рассылки
    // val savedButtons: List<Long> = listOf()

)