package ru.idfedorov09.telegram.bot.data.model

import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.mephi.sno.libs.flow.belly.Mutable

/**
 * Хранит актуализированную информацию о пользователе
 */
@Mutable
data class UserActualizedInfo(
    // null id - юзер не зарегистрирован
    val id: Long? = null,
    val tui: String,
    val lastTgNick: String? = null,
    val fullName: String? = null,
    val studyGroup: String? = null,
    val categories: MutableSet<Category> = mutableSetOf(),
    val roles: MutableSet<UserRole> = mutableSetOf(),
    var lastUserActionType: LastUserActionType? = null,

    /** Последний активный вопрос, заданный пользователем **/
    // TODO: readme -> активный вопрос - вопрос с диалогом
    val activeQuest: Quest?,

    /** Временные данные **/
    var data: String? = null,

    val isRegistered: Boolean,

    /** Текущая создаваемая рассылка пользователя **/
    var bcData: Broadcast? = null,

    /** Текущая выставленная юзеру клавиатура **/
    val currentKeyboardType: UserKeyboardType
)
