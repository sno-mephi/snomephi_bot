package ru.idfedorov09.telegram.bot.data.model

import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole

/**
 * Хранит актуализированную информацию о пользователе
 */
data class UserActualizedInfo(
    // null id - юзер не зарегистрирован
    val id: Long? = null,
    val tui: String,
    val lastTgNick: String? = null,
    val fullName: String? = null,
    val studyGroup: String? = null,
    val categories: MutableSet<Category> = mutableSetOf(),
    val roles: MutableSet<UserRole> = mutableSetOf(),
    val lastUserActionType: LastUserActionType? = null,

    /** Последний активный вопрос, заданный пользователем **/
    // TODO: readme -> активный вопрос - вопрос с диалогом
    val activeQuest: Quest?,
)