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
    QUEST_BAN("quest_ban|%d"),
    QUEST_START_DIALOG("quest_start_dialog|%d"),

    CATEGORY_ACTION_MENU("category_action_menu"),
    CATEGORY_EDIT("category_edit"),
    CATEGORY_ADD("category_add"),
    CATEGORY_DELETE("category_delete"),
    CATEGORY_PAGE("category_page|%d"),
    CATEGORY_CHOOSE("category_choose|%d"),
    CATEGORY_CONFIRM("category_confirm|%d"),
    
    USER_EDIT("user_edit"),
    USER_DELETE("user_delete"),
    USER_CONFIRM("user_confirm|%s"),
    USER_DECLINE("user_decline|%s"),
    USER_WITHOUT_GROUP("user_without_group")
    ;

    fun format(vararg args: Any?): String {
        return data.format(*args)
    }

    fun isMatch(callbackData: String): Boolean {
        var index = data.indexOf("%")
        if(index < 0){
            index = data.length
        }
        val prefix = data.substring(
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
