package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class TextCommands(

    /** текст команды **/
    val commandText: String,

    /** роли которым доступна эта команда **/
    private val allowedRoles: List<UserRole> = listOf(UserRole.USER),
) {
    TEST_COMMAND_1(
        "Тестовая команда 1",
    ),

    TEST_COMMAND_2(
        "Тестовая команда 2",
        listOf(UserRole.CATEGORY_BUILDER),
    ),

    QUEST_DIALOG_CLOSE(
        "❌ Завершить диалог",
    ),

    CATEGORY_CHOOSE_ACTION(
        "/category",
        listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
    ),
    CATEGORY_EDIT(
        "/category_edit",
        listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
    ),
    CATEGORY_ADD(
        "/category_add",
        listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
    ),
    CATEGORY_DELETE(
        "/category_delete",
        listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
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
