package ru.idfedorov09.telegram.bot.data.model

data class CloseDialogMessages(
    // Сообщения, когда пользователь закрывает диалог
    val onAuthorClose: CloseDialogMessagesPrimary = CloseDialogMessagesPrimary(
        toResponder = "<i>\uD83D\uDD18 Пользователь завершил диалог.</i>",
        toAuthor = "<b>Диалог завершен.</b>",
    ),
    // Сообщения, когда респондер закрывает диалог
    val onResponderClose: CloseDialogMessagesPrimary = CloseDialogMessagesPrimary(
        toResponder = "\uD83D\uDDA4 Спасибо за обратную связь! <b>Диалог завершен.</b>",
        toAuthor = "<i>\uD83D\uDD18 Оператор завершил диалог.</i>",
    ),
    // Сообщения в консоли по завершению действия; null - дефолтное
    val consoleResultText: String? = null,
)
