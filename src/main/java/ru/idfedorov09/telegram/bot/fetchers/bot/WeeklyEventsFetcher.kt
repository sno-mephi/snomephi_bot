package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 фетчер для рассылки мероприятий недели по команде
 */
@Component
class WeeklyEventsFetcher(
    private val broadcastRepository: BroadcastRepository,
) : GeneralFetcher() {


    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        val params = Params(
            bot,
            userActualizedInfo,
            update,
        )
        messageText.apply {
            when {
                startsWith(TextCommands.WEEKLY_EVENTS.commandText) -> sendWeeklyEvents(params)
            }
        }
    }


    private fun sendWeeklyEvents(params: Params) {
        val firstActiveWeeklyBroadcast = broadcastRepository.findFirstActiveWeeklyBroadcast()
        if (firstActiveWeeklyBroadcast == null) {
            params.bot.execute(
                SendMessage().also {
                    it.chatId = params.userActualizedInfo.tui
                    it.text = "Мероприятия недели пока не заполнены."
                    it.parseMode = ParseMode.HTML
                },
            )
        }
        else{
            params.bot.execute(
                SendMessage().also {
                    it.chatId = params.userActualizedInfo.tui
                    it.text =  firstActiveWeeklyBroadcast.text.toString()
                    it.parseMode = ParseMode.HTML
                },
            )
        }
    }

    private data class Params(
        val bot: Executor,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )

    }

