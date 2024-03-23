package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo

enum class TextCommands(
    /** текст команды **/
    val commandText: String,
    /** роли которым доступна эта команда **/
    val allowedRoles: List<UserRole> = listOf(UserRole.USER),
    val description: String? = "",
    val isFullCommand: Boolean = true,
) {
    QUEST_DIALOG_CLOSE(
        commandText = "❌ Завершить диалог",
        description = "завершает диало",
        isFullCommand = false,
    ),

    /** Открыть меню категорий **/
    CATEGORY_CHOOSE_ACTION(
        commandText = "/category",
        allowedRoles =
            listOf(
                UserRole.CATEGORY_BUILDER
            ),
        description = "настройка категорий",
    ),

    CATEGORY_CHOOSE_TEXT_ACTION(
        commandText = "Настройка категорий",
        allowedRoles =
        listOf(
            UserRole.CATEGORY_BUILDER
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
        commandText = "/userinfo",
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
        allowedRoles =
            listOf(
                UserRole.MAILER,
                UserRole.ROOT,
            ),
        description = "рассылка уведомлений - открывает конструктор рассылки уведомлений для дальнейшей настройки",
    ),

    WEEKLY_EVENTS(
        commandText = "Мероприятия недели",
        description = "присылает информацию о всех мероприятиях, запланированных на текущую неделю",
    ),

    PERMISSIONS_SETUP(
        commandText = "Выдача ролей",
        allowedRoles = listOf(UserRole.ROOT),
        description = "выдача и отзыв ролей у пользователей",
    ),

    HELP_COMMAND(
        commandText = "/help",
        description = "узнать все актуальные команды",
    ),

    RESET(
        commandText = "/reset",
        description = "удаление своего аккаунта",
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
