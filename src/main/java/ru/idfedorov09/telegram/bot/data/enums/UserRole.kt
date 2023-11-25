package ru.idfedorov09.telegram.bot.data.enums

/**
 * Доступнве пользовательские роли
 */
enum class UserRole(

    /** описание роли **/
    val roleName: String,

    /** описание роли **/
    val roleDescription: String,
) {
    ROOT(
        "ROOT",
        "полный доступ",
    ),

    USER(
        "USER",
        "обычный доступ, выдается после регистрации",
    ),

    BANNED(
        "BANNED",
        "бан",
    ),

    MAILER(
        "MAILER",
        "возможность делать рассылку",
    ),

    CATEGORY_BUILDER(
        "CATEGORY_BUILDER",
        "возможность добавлять новые категории рассылки",
    ),
}
