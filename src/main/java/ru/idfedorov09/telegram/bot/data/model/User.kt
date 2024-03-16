package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.converter.LastUserActionTypeConverter
import ru.idfedorov09.telegram.bot.data.model.converter.UserKeyboardTypeConverter

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
    @Column(name = "last_action_type", columnDefinition = "TEXT")
    @Convert(converter = LastUserActionTypeConverter::class)
    val lastUserActionType: LastUserActionType? = null,
    /** айди диалога (quest), который сейчас идет **/
    @Column(name = "quest_dialog_id")
    val questDialogId: Long? = null,
    @Column(name = "is_registered")
    val isRegistered: Boolean = false,
    @Column(name = "last_constructor_id")
    val constructorId: Long? = null,
    @Column(name = "is_deleted")
    val isDeleted: Boolean = false,
    /** тип текущей инлайн клавиатуры **/
    @Column(name = "current_keyboard_type", columnDefinition = "TEXT", updatable = false)
    @Convert(converter = UserKeyboardTypeConverter::class)
    val currentKeyboardType: UserKeyboardType = UserKeyboardType.WITHOUT_KEYBOARD,
    /** Было ли выполнено переключение клавиатуры на новую **/
    @Column(name = "is_keyboard_switched", updatable = false)
    val isKeyboardSwitched: Boolean = false,
)
