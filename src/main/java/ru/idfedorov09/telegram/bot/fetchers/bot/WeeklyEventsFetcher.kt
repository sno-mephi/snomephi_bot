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
фетчер для рассылки мероприятий недели по команде
 */
@Component
class WeeklyEventsFetcher(
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

        if (messageText.startsWith(TextCommands.WEEKLY_EVENTS.commandText)) {
            sendWeeklyEvents(userActualizedInfo)
        }
    }

    private fun sendWeeklyEvents(userActualizedInfo: UserActualizedInfo) {
        val firstActiveWeeklyBroadcast = broadcastRepository.findFirstActiveWeeklyBroadcast()
        firstActiveWeeklyBroadcast ?: run {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = "Мероприятия недели пока не заполнены.",
                ),
            )
            return
        }

        broadcastSenderService.sendBroadcast(
            userId = userActualizedInfo.id!!,
            broadcast = firstActiveWeeklyBroadcast,
            shouldAddToReceived = false,
        )
    }
}
