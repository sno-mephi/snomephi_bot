package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole

@Entity
@Table(name = "users_table")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    /** id юзера в телеграме **/
    @Column(name = "tui", unique = true)
    val tui: String? = null,

    /** последний сохраненный ник в телеге **/
    @Column(name = "last_tg_nick")
    val lastTgNick: String? = null,

    /** ФИО **/
    @Column(name = "full_name")
    val fullName: String? = null,

    /** учебная группа **/
    @Column(name = "study_group")
    val studyGroup: String? = null,

    /** id рассылок, на которые подписан юзер **/
    @Column(name = "categories")
    val categories: MutableSet<Long> = mutableSetOf(),

    /** поле для временных данных юзера **/
    @Column(name = "data", columnDefinition = "TEXT")
    val data: String? = null,

    /** роли **/
    @Enumerated(EnumType.STRING)
    @Column(name = "roles")
    val roles: MutableSet<UserRole> = mutableSetOf(),

    /** тип предыдущего действия пользователя **/
    @Enumerated(EnumType.STRING)
    @Column(name = "last_action_type")
    val lastUserActionType: LastUserActionType? = null,

    /** айди диалога (quest), который сейчас идет **/
    @Column(name = "quest_dialog_id")
    val questDialogId: Long? = null,

    @Column(name = "is_registered")
    val isRegistered: Boolean = false,

    @Column(name = "last_constructor_id")
    val constructorId: Long? = null,
)
