package ru.idfedorov09.telegram.bot.data.enums

enum class QuestionStatus {
    /** общается прямо сейчас **/
    DIALOG,

    /** завершен **/
    CLOSED,

    /** ждет ответа **/
    WAIT,
}
