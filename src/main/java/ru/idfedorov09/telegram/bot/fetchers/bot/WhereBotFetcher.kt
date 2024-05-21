package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class WhereBotFetcher(
    private val messageSenderService: MessageSenderService,
    private val updatesUtil: UpdatesUtil,
) : DefaultFetcher() {

    @InjectData
    @FetcherPerms(UserRole.ROOT)
    fun doFetch(update: Update, userActualizedInfo: UserActualizedInfo) {
        if (update.hasMessage() && update.message.hasText() && update.message.text == TextCommands.WHERE_BOT()) {
            val currentChatId = updatesUtil.getChatId(update) ?: return
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = currentChatId,
                    text = "Бот находится в чате с id=<code>$currentChatId</code>",
                    parseMode = ParseMode.HTML,
                )
            )
        }
    }
}