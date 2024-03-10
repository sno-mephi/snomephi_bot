package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.service.BroadcastSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 фетчер для рассылки мероприятий недели по команде
 */
@Component
class WeeklyEventsFetcher(
    private val broadcastRepository: BroadcastRepository,
    private val broadcastSenderService: BroadcastSenderService
) : GeneralFetcher() {


    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.WEEKLY_EVENTS.commandText))
            sendWeeklyEvents(bot, userActualizedInfo)
    }


    private fun sendWeeklyEvents(
        bot: Executor,
        userActualizedInfo: UserActualizedInfo
    ) {
        val firstActiveWeeklyBroadcast = broadcastRepository.findFirstActiveWeeklyBroadcast()
        firstActiveWeeklyBroadcast ?: run {
            bot.execute(
                SendMessage().also {
                    it.chatId = userActualizedInfo.tui
                    it.text = "Мероприятия недели пока не заполнены."
                },
            )
            return
        }

        broadcastSenderService.sendBroadcast(
            userId = userActualizedInfo.id!!,
            broadcast = firstActiveWeeklyBroadcast,
            shouldAddToReceived = false
        )
    }

}

