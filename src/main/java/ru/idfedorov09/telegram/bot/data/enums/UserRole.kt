package ru.idfedorov09.telegram.bot.data.enums

/**
 * Доступнве пользовательские роли
 */
enum class UserRole(
    /** описание роли **/
    val description: String,
) {
    ROOT(
        "полный доступ",
    ),

    USER(
        "обычный доступ, выдается после регистрации",
    ),

    MODERATOR(
        "возможность выдавать баны",
    ),

    MAILER(
        "возможность делать рассылку",
    ),

    CATEGORY_BUILDER(
        "возможность добавлять новые категории рассылки",
    ),

    SNO_TEAM(
        "член совета СНО",
    ),
}
