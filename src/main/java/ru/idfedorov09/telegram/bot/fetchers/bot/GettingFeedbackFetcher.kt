package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.service.BroadcastSenderService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData

/**
    фетчер для получения сообщения с обратной связью
 */
@Component
class GettingFeedbackFetcher(
    private val broadcastRepository: BroadcastRepository,
    private val broadcastSenderService: BroadcastSenderService,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {

    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.FEEDBACK_BUTTON())) {
            sendFeedbackPage(userActualizedInfo)
        }
    }

    private fun sendFeedbackPage(userActualizedInfo: UserActualizedInfo) {
        val feedbackPage = broadcastRepository.findFeedbackPage() ?: run {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = "Страница обратной связи еще не заполнена.",
                ),
            )
            return
        }

        broadcastSenderService.sendBroadcast(
            userId = userActualizedInfo.id!!,
            broadcast = feedbackPage,
            shouldAddToReceived = false,
        )
    }
}