package ru.idfedorov09.telegram.bot.data.enums

/**
 * О работе с каллбеками (кнопками) читай подробнее тут: TODO()
 * ВНИМАНИЕ! Максимальная длина callbackData - 64 символа, это надо учитывать
 */
enum class CallbackCommands(
    val data: String,
) {
    VOID("void"),

    /** Нажата кнопка Ответ **/
    QUEST_ANSWER("quest_ans|%d"),
    QUEST_IGNORE("quest_ignore|%d"),
    QUEST_RECREATE("quest_recreate|%d"),
    QUEST_RECREATE_START_DIALOG("quest_recreate_start_dialog|%d"),
    QUEST_SHOW_HISTORY("quest_show_history|%d"),
    QUEST_START_DIALOG("quest_start_dialog|%d"),

    CATEGORY_ACTION_MENU("category_action_menu|%d"),

    /** страница категорий **/
    CATEGORY_CHOOSE_MENU("category_choose_menu|%d"),
    CATEGORY_EDIT("category_edit"),
    CATEGORY_ADD("category_add"),
    CATEGORY_DELETE("category_delete"),

    /** страница категорий**/
    CATEGORY_PAGE("category_page|%d"),

    /** id категории, страница категорий**/
    CATEGORY_CHOOSE("category_choose|%d|%d"),

    /** id категории **/
    CATEGORY_CONFIRM("category_confirm|%d"),
    CATEGORY_INPUT_CANCEL("category_input_cancel"),

    /** bool можно ли снять категорию **/
    CATEGORY_IS_UNREMOVABLE("category_is_unremovable|%d"),

    CATEGORY_EXIT("category_exit"),

    /** Блок регистрации **/

    REGISTRATION_CONFIRM_FULL_NAME("registration_confirm_full_name"),
    REGISTRATION_DECLINE_FULL_NAME("registration_decline_full_name"),
    REGISTRATION_CONFIRM_STUDY_GROUP("registration_confirm_study_group"),
    REGISTRATION_DECLINE_STUDY_GROUP("registration_decline_study_group"),
    REGISTRATION_WITHOUT_STUDY_GROUP("registration_without_study_group"),

    /** Блок банов **/

    BANNED_USER("ban_user|%d|%d"),
    BANNED_CANCEL("ban_cancel"),
    BANNED_PERMANENT("ban_permanent"),
    BANNED_CONFIRM("ban_confirm"),

    UNBANNED_USER("unban_user|%d"),
    UNBANNED_CONFIRM("unban_confirm|%d"),

    /** Блок конструктора рассылок **/

    BROADCAST_CANCEL("#bc_cancel"),
    BROADCAST_CHANGE_TEXT("#bc_change_text"),
    BROADCAST_CHANGE_PHOTO("#bc_change_photo"),
    BROADCAST_TO_SCHEDULE_CONSOLE("#bc_to_schedule_console"),
    BROADCAST_DELETE_PHOTO("#bc_delete_photo"),
    BROADCAST_CHANGE_CATEGORIES("#bc_change_categories"),
    BROADCAST_ACTION_CANCEL("#bc_action_cancel"),
    BROADCAST_PREVIEW("#bc_preview"),
    BROADCAST_SEND_NOW("#bc_send_now"),
    BROADCAST_ADD_BUTTON("#bc_add_button"),
    BROADCAST_CHANGE_BUTTON_CAPTION("#bc_change_button_caption"),
    BROADCAST_CHANGE_BUTTON_LINK("#bc_change_button_link"),
    BROADCAST_ACTION_SHOW_BTN_CONSOLE("#bc_action_show_btn_console"),
    BROADCAST_CHANGE_BUTTON_WITH_ID("#bc_change_button_with_id"),
    BROADCAST_BUTTON_REMOVE("#bc_button_remove"),
    BROADCAST_CHANGE_BUTTON_CALLBACK("#bc_change_button_callback"),
    BROADCAST_COMPLETE("#bc_complete"),
    BROADCAST_START_COMMON("#bc_start_common"),
    BROADCAST_START_WEEKLY("#bc_start_weekly"),
    BROADCAST_WB_PREVIEW_STATE("#bc_web_preview_state"),

    /** Блок настройки юзера **/

    SETTING_USER_CHANGE_FULL_NAME("setting_user_full_name"),
    SETTING_USER_CHANGE_STUDY_GROUP("setting_user_study_group"),
    SETTING_USER_WITHOUT_STUDY_GROUP("setting_user_without_study_group"),
    SETTING_USER_BACK_TO_CONSOLE("setting_user_back_to_console"),

    /** Блок конструктора опроса **/

    SURVEY_NEW_QUESTION("survey_new_question"),
    SURVEY_SHOW_QUESTIONS("survey_show_questions"),
    SURVEY_ORDER_QUESTION("survey_order_question"),
    SURVEY_CANCEL("survey_cancel"),
    ;

    fun format(vararg args: Any?): String {
        return data.format(*args)
    }

    fun isMatch(callbackData: String): Boolean {
        var index = data.indexOf("%")
        if (index < 0) {
            index = data.length
        }
        val prefix =
            data.substring(
                0,
                index,
            )
        return callbackData.startsWith(prefix)
    }

    companion object {
        fun params(data: String): List<String> {
            return data.split('|').let {
                it.subList(1, it.size)
            }
        }
    }
}
