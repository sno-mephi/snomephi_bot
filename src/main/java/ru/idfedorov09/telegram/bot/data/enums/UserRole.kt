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
        "бог",
        "полный доступ",
    ),

    USER(
        "пользователь",
        "обычный доступ, выдается после регистрации",
    ),

    BANNED(
        "бан",
        "бан",
    ),

    MAILER(
        "почтальон",
        "возможность делать рассылку",
    ),

    CATEGORY_BUILDER(
        "категоричный",
        "возможность добавлять новые категории рассылки",
    ),
}
