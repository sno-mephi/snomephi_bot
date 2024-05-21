package ru.idfedorov09.telegram.bot.data.enums

import ru.idfedorov09.telegram.bot.data.model.User
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
                UserRole.CATEGORY_BUILDER,
            ),
        description = "настройка категорий",
    ),

    CATEGORY_CHOOSE_TEXT_ACTION(
        commandText = "Настройка категорий",
        allowedRoles =
            listOf(
                UserRole.CATEGORY_BUILDER,
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

    SURVEY_CONSTRUCTOR(
        commandText = "Рассылка опроса",
        allowedRoles =
            listOf(
                UserRole.MAILER,
                UserRole.ROOT,
            ),
        description = "открывает конструктор рассылки опросов для дальнейшей настройки"
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
    BUG_COMMAND(
        commandText = "/bug",
        description = "отправить сообщение о баге: /bug <текст отбращения>",
    ),

    BANNED_COMMAND(
        commandText = "/ban",
        description = "Забанить пользователя по tui",
        allowedRoles =
            listOf(
                UserRole.ROOT,
                UserRole.MODERATOR,
            ),
    ),

    UNBANNED_COMMAND(
        commandText = "/unban",
        description = "Разбанить пользователя по tui",
        allowedRoles =
            listOf(
                UserRole.ROOT,
                UserRole.MODERATOR,
            ),
    ),

    USER_SETTING(
        commandText = "/setting",
        description = "Настройки пользователя",
    ),

    SURVEY_QUESTION(
        commandText = "/show_question",
        isFullCommand = false,
        allowedRoles = listOf(
            UserRole.MAILER,
            UserRole.ROOT,
        ),
    ),

    CONFIG_PARAMS(
        commandText = "/config",
        description = "Конфигурация параметров бота",
        allowedRoles = listOf(UserRole.USER),
    ),

    WHERE_BOT(
        commandText = "/where_bot",
        description = "Показывает информацию о чате, в котором находится бот",
        allowedRoles = listOf(UserRole.ROOT),
    )
    ;

    /** Проверяет, является ли текст командой **/
    companion object {
        fun isTextCommand(text: String?) = entries.map { it.commandText }.any { text?.startsWith(it) ?: false }
    }

    fun isAllowed(user: UserActualizedInfo) = isAllowed(user.roles)
    fun isAllowed(user: User) = isAllowed(user.roles)

    private fun isAllowed(roles: Set<UserRole>): Boolean {
        if (roles.contains(UserRole.ROOT)) return true
        return roles.map { this.allowedRoles.contains(it) }.firstOrNull { it } ?: false
    }

    operator fun invoke() = commandText
}
