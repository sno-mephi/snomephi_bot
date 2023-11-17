package ru.idfedorov09.telegram.bot.data.enums

enum class TextCommands(val text: String) {
    TEST_COMMAND_1("Тестовая команда 1"),
    TEST_COMMAND_2("Тестовая команда 2"),
    ;

    /** Проверяет, является ли текст командой **/
    companion object {
        fun isTextCommand(text: String) = entries.map { it.text }.contains(text)
    }
}
