package ru.idfedorov09.telegram.bot.data.enums

enum class RegistrationMessageText(private val text: String) {
    FullNameRequest("Пожалуйста, введите свое ФИО"),
    GroupRequest("Введите свою группу"),

    FullNameConfirmation("Вы действительно хотите использовать ФИО %s?"),
    GroupConfirmation("Вы действительно состоите в группе %s?"),
    WithoutGroupConfirmation("Вы действительно не из МИФИ?"),

    InvalidGroup("Кажется такой группы не существует, пожалуйста, введите свою группу заново"),
    InvalidFullName("Кажется Вы ввели ФИО неправильно. Используйте только символы из кириллицы и пробелы"),
    AlreadyExists("Такой пользователь уже зарегистрирован под юзернеймом @%s"),

    RegistrationComplete("Спасибо, Вы зарегистрированы"),
    Welcome("Здравствуйте, %s"),
    RegistrationStart("Здравствуйте! Вы не зарегистрированы");


    operator fun invoke(extraText: String = "") = text + extraText

    fun format(parameter: String) = text.format(parameter)

}