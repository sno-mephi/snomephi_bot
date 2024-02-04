package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class TextCommands(

    /** текст команды **/
    val commandText: String,

    /** роли которым доступна эта команда **/
    private val allowedRoles: List<UserRole> = listOf(UserRole.USER),
) {

    QUEST_DIALOG_CLOSE(
        "❌ Завершить диалог",
    ),

    /** Открыть меню категорий **/
    CATEGORY_CHOOSE_ACTION(
        "/category",
        listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
    ),

    SETTING_MAIL(
        "Настройка рассылки",
    ),

    TOGGLE(
        "/toggle",
    ),

    USER_INFO(
        "/userInfo",
        listOf(UserRole.ROOT),
    ),

    ROLE_DESCRIPTION(
        "/role",
        listOf(UserRole.ROOT),
    ),
    ;

    /** Проверяет, является ли текст командой **/
    companion object {
        fun isTextCommand(text: String?) = entries.map { it.commandText }.any { text?.startsWith(it) ?: false }
    }

    fun isAllowed(user: UserActualizedInfo): Boolean {
        if (user.roles.contains(UserRole.ROOT)) return true
        return user.roles.map { this.allowedRoles.contains(it) }.firstOrNull { it } ?: false
    }
}
