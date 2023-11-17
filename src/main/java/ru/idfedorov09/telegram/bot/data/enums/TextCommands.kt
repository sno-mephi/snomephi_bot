package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class TextCommands(

    /** текст команды **/
    private val text: String,

    /** роли которым доступна эта команда **/
    private val allowedRoles: List<UserRole> = listOf(UserRole.USER),
) {
    TEST_COMMAND_1(
        "Тестовая команда 1",
        listOf(UserRole.ROOT),
    ),

    TEST_COMMAND_2(
        "Тестовая команда 2",
        listOf(UserRole.ROOT),
    ),

    ;

    /** Проверяет, является ли текст командой **/
    companion object {
        fun isTextCommand(text: String) = entries.map { it.text }.contains(text)
    }

    fun isAllowed(user: UserActualizedInfo): Boolean {
        if (user.roles.contains(UserRole.ROOT)) return true
        return user.roles.map { this.allowedRoles.contains(it) }.firstOrNull { it } ?: false
    }
}
