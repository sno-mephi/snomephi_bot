package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.ECallbackData
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class ConfigParams(
    /** Название отображаемое на кнопке **/
    val displayName: String,
    /** Дефолтное значение **/
    val defaultValues: List<String>,
    /** Ключ **/
    val key: String,
    /** Тип параметра **/
    val type: ConfigParamType,
    /** Кнопки для выбора из нескольких вариантов ответа **/
    val selectList: List<ECallbackData> = listOf(),
    val allowedRoles: List<UserRole> = listOf()
) {
    /** айди чата админов **/
    ADMIN_CHAT_ID(
        displayName = "id чата админов",
        defaultValues = listOf("-1002057270905"),
        key = "admin_chat_id",
        type = ConfigParamType.INPUT,
    )
    ;

    companion object {
        fun getByKey(key: String?) = key?.let { entries.find { it.key == key } }
    }

    fun isAllowed(user: UserActualizedInfo) = isAllowed(user.roles)
    fun isAllowed(user: User) = isAllowed(user.roles)

    private fun isAllowed(roles: Set<UserRole>): Boolean {
        if (roles.contains(UserRole.ROOT)) return true
        return roles.map { this.allowedRoles.contains(it) }.firstOrNull { it } ?: false
    }
}