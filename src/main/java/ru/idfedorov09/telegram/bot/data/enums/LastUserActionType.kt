package ru.idfedorov09.telegram.bot.data.enums

/**
 * Тип последнего действия пользователя
 */
enum class LastUserActionType {
    DEFAULT,

    /** Нажата кнопка ОТВЕТ **/
    ACT_QUEST_ANS_CLICK,

    /** Завершил диалог **/
    ACT_QUEST_DIALOG_CLOSE,
}
