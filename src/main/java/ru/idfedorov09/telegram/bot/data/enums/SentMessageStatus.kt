package ru.idfedorov09.telegram.bot.data.enums

enum class SentMessageStatus(
    val description: String
) {
    DEFAULT_STATUS("Создано"),
    OK("Успешно отправлено"),
    TIMEOUT("Превышено ограничение по отправке собщения в единицу времени"),
    UNKNOWN_ERROR("Неизвестная ошибка при отправке сообщения"),

    ;
}