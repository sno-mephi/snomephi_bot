package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
фетчер для рассылки команд /help
 */
@Component
class HelpCommandFetcher() : GeneralFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.HELP_COMMAND.commandText)) {

            var finalText = ""
            for (command in TextCommands.values()) {
                if (command.isFullCommand && command.allowedRoles.intersect(userActualizedInfo.roles).isNotEmpty()) {
                    finalText += "▪"+ command.commandText+ " - " + command.description + "\n"
                }
            }

            bot.execute(
                SendMessage().also {
                    it.chatId = userActualizedInfo.tui
                    it.text = finalText
                    it.parseMode = ParseMode.HTML
                },
            )
        }
    }
}
