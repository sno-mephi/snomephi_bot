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
    /** список юзеров, на которых словили исключение при отправке бродкаста **/
    @Column(name = "failed_users_id")
    val failedUsersId: MutableList<Long> = mutableListOf(),
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
    val categoriesId: MutableSet<Long> = mutableSetOf(),
    /** название рассылки **/
    @Column(name = "broadcast_name", columnDefinition = "TEXT")
    val name: String? = null,
    /** флаг отложенной рассылки **/
    @Column(name = "is_scheduled")
    val isScheduled: Boolean = false,
    /** флаг завершения рассылки **/
    @Column(name = "is_built")
    val isBuilt: Boolean = false,
    /** флаг мероприятий недели **/
    @Column(name = "is_weekly")
    val isWeekly: Boolean = false,
    /** последнее сообщение с консолью редактирования (в лс автора) **/
    /** нужно для редактирования рассылки в bc **/
    @Column(name = "last_console_message_id")
    val lastConsoleMessageId: Int? = null,
    /** удалена ли эта рассылка **/
    /** true только в случае если юзер отменяет создание рассылки **/
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = false,
)
