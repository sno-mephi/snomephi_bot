package ru.idfedorov09.telegram.bot.data.enums

/**
 * О работе с каллбеками (кнопками) читай подробнее тут: TODO()
 * ВНИМАНИЕ! Максимальная длина callbackData - 64 символа, это надо учитывать
 */
enum class CallbackCommands(
    val data: String,
) {
    /** Нажата кнопка Ответ **/
    QUEST_ANSWER("quest_ans|%d"),
    QUEST_IGNORE("quest_ignore|%d"),
    QUEST_BAN("quest_ban|%d"),
    QUEST_START_DIALOG("quest_start_dialog|%d"),

    ;

    fun format(vararg args: Any?): String {
        return data.format(*args)
    }

    fun isMatch(callbackData: String): Boolean {
        val prefix = data.substring(
            0,
            data.indexOf("%"),
        )
        return callbackData.startsWith(prefix)
    }

    fun params(): List<String> {
        return data.split('|').let {
            it.subList(1, it.size)
        }
    }
}
