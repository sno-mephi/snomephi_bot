package ru.idfedorov09.telegram.bot.data.enums

enum class UserStrings(private val text: String) {
    FullNameRequest("Пожалуйста, введите свое ФИО"),
    GroupRequest("Введите свою группу"),

    InvalidGroup("Кажется такой группы не существует, пожалуйста, введите свою группу заново"),
    InvalidFullName("Кажется вы ввели ФИО неправильно"),
    AlreadyExists("Такой пользователь уже зарегистрирован"),

    RegistrationComplete("Спасибо, Вы зарегистрированы"),
    Welcome("Здравствуйте"),
    RegistrationStart("Здравствуйте! Вы не зарегистрированы");


    operator fun invoke(extraText: String = "") = text + extraText

}