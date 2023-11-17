package ru.idfedorov09.telegram.bot.data.model

import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole

/**
 * Хранит актуализированную информацию о пользователе
 */
data class UserActualizedInfo(
    val id: Long,
    val tui: String,
    val lastTgNick: String = "default-nick",
    val fullName: String? = null,
    val studyGroup: String? = null,
    val categories: MutableSet<Category> = mutableSetOf(),
    val roles: MutableSet<UserRole> = mutableSetOf(),
    val lastUserActionType: LastUserActionType? = null,
)