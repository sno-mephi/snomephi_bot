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

    /**Отправлена inline клавиатура выбора действия с категорией**/
    @Deprecated("Раньше использовалось неправильно; устарело")
    CATEGORY_ACTION_CHOOSING,

    /**Отправлена inline клавиатура создания категории**/
    CATEGORY_ADDING,

    /**Отправлена inline клавиатура удаления категории**/
    CATEGORY_DELETING,

    /**Отправлена inline клавиатура изменения категории**/
    CATEGORY_EDITING,

    /**Запущена последовательность ввода категории**/
    CATEGORY_INPUT_START,

    /**Введено название категории**/
    CATEGORY_INPUT_TITLE,

    /**Введен тэг категории**/
    CATEGORY_INPUT_SUFFIX,

    /**Введено описание категории**/
    CATEGORY_INPUT_DESCRIPTION,

    /** действия при регистраци **/
    REGISTRATION_START,

    REGISTRATION_ENTER_FULL_NAME,

    REGISTRATION_ENTER_GROUP,

    REGISTRATION_CONFIRM_FULL_NAME,

    REGISTRATION_CONFIRM_GROUP,


    /**
     * Конструктор рассылки
     */

    /** ввод текста рассылки **/
    BC_TEXT_TYPE,

}
