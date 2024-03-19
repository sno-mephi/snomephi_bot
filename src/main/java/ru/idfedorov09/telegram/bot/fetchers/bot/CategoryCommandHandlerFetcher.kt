package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий комманды категорий
 */
@Component
class CategoryCommandHandlerFetcher(
    private val messageSenderService: MessageSenderService,
    private val updatesUtil: UpdatesUtil,
) : DefaultFetcher() {
    private data class RequestData(
        val chatId: String,
        val update: Update,
        var userInfo: UserActualizedInfo,
    )

    @InjectData
    @FetcherPerms(UserRole.CATEGORY_BUILDER)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (update.message == null || !update.message.hasText()) return userActualizedInfo
        val messageText = update.message.text
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData =
            RequestData(
                chatId,
                update,
                userActualizedInfo,
            )
        when (messageText) {
            TextCommands.CATEGORY_CHOOSE_ACTION.commandText ->
                commandChooseAction(requestData)
        }
        return requestData.userInfo
    }

    private fun commandChooseAction(data: RequestData) {
        if (TextCommands.CATEGORY_CHOOSE_ACTION.isAllowed(data.userInfo)) {
            sendMessage(
                data,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        } else {
            data.userInfo =
                data.userInfo.copy(
                    lastUserActionType = LastUserActionType.DEFAULT,
                )
            sendMessage(
                data,
                "🔒 Действие недоступно для вас",
            )
        }
    }

    private fun sendMessage(
        data: RequestData,
        text: String,
    ) {
        val lastSent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = data.chatId,
                    text = text,
                ),
            ).chatId
        data.userInfo =
            data.userInfo.copy(
                data = lastSent.toString(),
            )
    }

    private fun sendMessage(
        data: RequestData,
        text: String,
        keyboard: InlineKeyboardMarkup,
    ) {
        val lastSent =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = data.chatId,
                    text = text,
                    replyMarkup = keyboard,
                ),
            ).messageId
        data.userInfo =
            data.userInfo.copy(
                data = lastSent.toString(),
            )
    }
}
