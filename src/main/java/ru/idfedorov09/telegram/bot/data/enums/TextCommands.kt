package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class TextCommands(
    /** текст команды **/
    val commandText: String,
    /** роли которым доступна эта команда **/
    private val allowedRoles: List<UserRole> = listOf(UserRole.USER),
    private val description: String? = "",
    private val isFullCommand: Boolean = true,
) {
    QUEST_DIALOG_CLOSE(
        commandText = "❌ Завершить диалог",
        description = "Завершает диало",
        isFullCommand = false,
    ),

    /** Открыть меню категорий **/
    CATEGORY_CHOOSE_ACTION(
        commandText = "/category",
        allowedRoles = listOf(
            UserRole.CATEGORY_BUILDER,
            UserRole.ROOT,
        ),
        description = "настройка категорий",
    ),

    SETTING_MAIL(
        commandText = "Настройка уведомлений",
        description = "помогает настроить рассылку нужных вам уведомлений о мероприятих и кружках",
    ),

    TOGGLE(
        commandText = "/toggle",
        isFullCommand = false,
    ),

    USER_INFO(
        commandText = "/userInfo",
        allowedRoles = listOf(UserRole.ROOT),
        description = "присылает полную информацию о пользователе",
    ),

    ROLE_DESCRIPTION(
        commandText = "/role",
        allowedRoles = listOf(UserRole.ROOT),
        description = "присылает полный список ролей пользователя",
    ),

    BROADCAST_CONSTRUCTOR(
        commandText = "Рассылка уведомлений",
        allowedRoles = listOf(
            UserRole.MAILER,
            UserRole.ROOT,
        ),
        description = "Рассылка уведомлений - открывает конструктор рассылки уведомлений для дальнейшей настройки",
    ),

    WEEKLY_EVENTS(
        commandText = "Мероприятия недели",
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

    operator fun invoke() = commandText
}
