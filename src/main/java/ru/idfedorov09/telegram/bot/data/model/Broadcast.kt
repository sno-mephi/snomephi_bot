package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "broadcast_table")
data class Broadcast(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    val id: Long? = null,

    /** текст сообщения рассылки **/
    @Column(name = "broadcast_text", columnDefinition = "TEXT")
    val text: String? = null,

    /** хеш картинок в рассылках **/
    @Column(name = "broadcast_image_hash", columnDefinition = "TEXT")
    val imageHash: String? = null,

    /** список юзеров, получивших рассылку **/
    @Column(name = "received_users_id")
    val receivedUsersId: MutableList<Long> = mutableListOf(),

    /** флаг завершения рассылки **/
    @Column(name = "is_completed")
    val isCompleted: Boolean = false,

    /** время начала рассылки **/
    @Column(name = "broadcast_start_dttm")
    val startTime: LocalDateTime? = null,

    /** время окончания рассылки, если null, то рассылка не завершена **/
    @Column(name = "broadcast_finish_dttm")
    val finishTime: LocalDateTime? = null,

    /** id создателя рассылки **/
    @Column(name = "broadcast_author_id")
    val authorId: Long? = null,

    /** список категорий, по которым идет рассылка **/
    @Column(name = "categories_id")
    val categoriesId: MutableList<Long> = mutableListOf(),

    /** название рассылки **/
    @Column(name = "broadcast_name", columnDefinition = "TEXT")
    val name: String? = null,

    /** флаг отложенной рассылки **/
    @Column(name = "is_scheduled")
    val isScheduled: Boolean = false,

    /** список кнопок в рассылке **/
    @Column(name = "buttons_id")
    val buttonsId: MutableList<Long> = mutableListOf(),

    /** флаг завершения рассылки **/
    @Column(name = "is_built")
    val isBuilt: Boolean = false,
)
