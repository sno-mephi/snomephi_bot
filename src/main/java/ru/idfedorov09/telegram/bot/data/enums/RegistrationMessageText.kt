package ru.idfedorov09.telegram.bot.data.enums

enum class RegistrationMessageText(private val text: String) {
    FullNameRequest("Пожалуйста, введите свое ФИО"),
    GroupRequest("Введите название Вашего СНО/СМУС"),

    FullNameConfirmation("Вы действительно хотите использовать ФИО %s?"),
    GroupConfirmation("Вы действительно состоите в СНО/СМУС %s?"),
    WithoutGroupConfirmation("Вы действительно не из МИФИ?"),

    InvalidFullName("Кажется Вы ввели ФИО неправильно. Используйте только символы из кириллицы и пробелы"),

    RegistrationComplete("Регистрация прошла успено. " +
            "\nВ боте есть команда /help, которая содержит список актуальных команд." +
            "\n\nВ случае сбоя бота, Вы можете воспользоваться командами /bug и /reset, " +
            "их полное описание есть в команде /help."),
    Welcome("Здравствуйте, %s"),
    RegistrationStart("Здравствуйте! Вы не зарегистрированы\nПожалуйста, введите свое ФИО"),
    ;

    operator fun invoke(extraText: String = "") = text + extraText

    fun format(parameter: String) = text.format(parameter)
}
