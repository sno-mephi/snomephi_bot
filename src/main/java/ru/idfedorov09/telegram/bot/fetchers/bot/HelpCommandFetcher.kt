package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher


/**
фетчер для рассылки команд /help
 */
@Component
class HelpCommandFetcher(
    private val messageSenderService: MessageSenderService,
) : GeneralFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.HELP_COMMAND.commandText)) {


            val finalText = TextCommands.values().filter { it.isFullCommand && it.allowedRoles.intersect(userActualizedInfo.roles).isNotEmpty() }.joinToString(separator = "\n") {
                "▪" + it.commandText+ " - " + it.description
            }

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = finalText,
                ),
            )
        }
    }
}
