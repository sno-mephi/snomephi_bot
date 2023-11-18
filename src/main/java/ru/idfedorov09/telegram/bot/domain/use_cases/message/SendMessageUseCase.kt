package ru.idfedorov09.telegram.bot.domain.use_cases.message

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.idfedorov09.telegram.bot.executor.Executor

@Component
class SendMessageUseCase {
    operator fun invoke(
        bot: Executor,
        text: String,
        chatId: String,
        replyToMessageId: Int? = null,
        disableNotification: Boolean = false,
        disableWebPagePreview: Boolean = false
    ) {
        bot.execute(
            SendMessage().apply {
                this.text = text
                this.chatId = chatId
                this.disableNotification = disableNotification
                this.disableWebPagePreview = disableWebPagePreview
                replyToMessageId?.let { this.replyToMessageId = it }
            }
        )
    }
}