package ru.idfedorov09.telegram.bot.data.enums

/**
 * Тип последнего действия пользователя
 */
enum class LastUserActionType(
    /** описание действия **/
    val actionDescription: String,
) {
    DEFAULT(
        "DEFAULT",
    ),

    /** Нажата кнопка ОТВЕТ **/
    ACT_QUEST_ANS_CLICK(
        "Нажата кнопка ОТВЕТ",
    ),

    /** Завершил диалог **/
    ACT_QUEST_DIALOG_CLOSE(
        "Завершил диалог",
    ),
}
