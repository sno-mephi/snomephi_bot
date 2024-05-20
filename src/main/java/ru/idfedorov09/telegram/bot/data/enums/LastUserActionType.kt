package ru.idfedorov09.telegram.bot.data.enums

/**
 * Тип последнего действия пользователя
 */
enum class LastUserActionType {
    DEFAULT,

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

    /** ввод фото рассылки **/
    BC_PHOTO_TYPE,

    /** Ввод текста кнопки **/
    BC_BUTTON_CAPTION_TYPE,

    /** Ввод ссылки кнопки **/
    BC_BUTTON_LINK_TYPE,

    /** Ввод текста коллбэка кнопки **/
    BC_BUTTON_CALLBACK_TYPING,

    /** ввод времени начала рассылки **/
    BC_CHANGE_START_TIME,

    /** изменение категорий рассылки **/
    BC_CHANGE_CATEGORIES,

    /** ввод tui человека для прав **/
    PERMS_ENTER_TUI,

    /** ввод tui человека для бана **/
    BANED_ENTER_TUI,

    /** ввод tui человека для бана **/
    UNBANED_ENTER_TUI,

    /** ввод причины бана **/
    BANNED_ENTER_REASON,

    /** ввод времени конца бана **/
    BANNED_ENTER_FINISH_TIME,

    /** ввод измененного ФИО **/
    SETTING_USER_ENTER_FULL_NAME,

    /** ввод измененной СНО/СМУС **/
    SETTING_USER_ENTER_STUDY_GROUP,

    /** начал создание вопроса в рассылке**/
    SURVEY_CREATE_QUESTION,

    /** выбрал тип вопроса **/
    SURVEY_QUESTION_CHOSEN_TYPE,

    /** начад менять текст вопроса в опросе**/
    SURVEY_QUESTION_CHANGE_TEXT,

    /** начад менять варианты ответов  опросе**/
    SURVEY_QUESTION_CHANGE_ANSWER_OPTIONS,

    /** начал менять стандартный порядок вопросов **/
    SURVEY_CHANGE_STANDARD_ORDER_QUESTIONS,

    SURVEY_START_ANSWER
}
