package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ban_table")
data class Ban(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    /** индентефикатор пользователя в БД **/
    @Column(name = "user_id")
    val userId: Long? = null,

    /** комментарий для указания причины бана **/
    @Column(name = "comment")
    val comment: String? = null,

    @Column(name = "datetime")
    val dateTime: LocalDateTime? = null,

)
