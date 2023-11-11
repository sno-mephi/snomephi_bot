package ru.idfedorov09.telegram.bot.data.enums

/**
 * Доступнве пользовательские роли
 */
enum class UserRole {
    /** полный доступ **/
    ROOT,

    /** обычный доступ, выдается после регистрации **/
    USER,

    /** бан **/
    BANNED,

    /** возможность делать рассылку **/
    MAILER,

    /** возможность добавлять новые категории рассылки **/
    CATEGORY_BUILDER,
}