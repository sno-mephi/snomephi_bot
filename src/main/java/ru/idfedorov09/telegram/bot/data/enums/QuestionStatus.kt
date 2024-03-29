package ru.idfedorov09.telegram.bot.data.enums

enum class QuestionStatus {
    /** общается прямо сейчас **/
    DIALOG,

    /** завершен **/
    CLOSED,

    /** ждет ответа **/
    WAIT,

    /** админ проигнорил вопрос **/
    IGNORE,

    /** админ забанил пользователя **/
    BLOCK_USER,
}
