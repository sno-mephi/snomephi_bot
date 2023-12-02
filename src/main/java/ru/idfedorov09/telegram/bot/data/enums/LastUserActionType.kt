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


    /** действия при регистраци **/
    REGISTRATION_START,

    REGISTRATION_ENTER_FULL_NAME,

    REGISTRATION_ENTER_GROUP,

    REGISTRATION_CONFIRM_FULL_NAME,

    REGISTRATION_CONFIRM_GROUP

}
