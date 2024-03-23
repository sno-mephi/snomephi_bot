package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import java.util.regex.Pattern

/**
фетчер для сообщения о баге /bug <текст обращения>
 */
@Component
class BugReportFetcher(
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.BUG_COMMAND.commandText)) {
            val pattern = Pattern.compile("/bug\\s+(.+)")
            val matcher = pattern.matcher(messageText)


            if (matcher.matches()) {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = GlobalConstants.QUEST_RESPONDENT_CHAT_ID,
                        text = "#bug от пользователя tui = ${userActualizedInfo.tui} \n" + matcher.group(1),
                    ),
                )

                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = "Ваше обращение отправлено. Если бот не работает из-за бага, воспользуйтесь командой /reset.",
                    ),
                )
                stopFlowNextExecution()
            } else {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = "Если вы хотите отправить сообщение о баге, то вы должны ввсети сообщение вида: /bug <текст обращения>",
                    ),
                )

            }
        }
    }
}