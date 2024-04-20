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

/**
 * Фетчер, обрабатывающий комманды категорий
 */
@Component
class CategoryCommandHandlerFetcher(
    private val messageSenderService: MessageSenderService,
    private val updatesUtil: UpdatesUtil,
) : DefaultFetcher() {
    private data class Params(
        val chatId: String,
        val update: Update,
        var userActualizedInfo: UserActualizedInfo,
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
        val params =
            Params(
                chatId,
                update,
                userActualizedInfo,
            )
        when (messageText) {
            TextCommands.CATEGORY_CHOOSE_ACTION() ->
                commandChooseAction(params)
            TextCommands.CATEGORY_CHOOSE_TEXT_ACTION() ->
                commandChooseAction(params)
        }
        return params.userActualizedInfo
    }

    private fun commandChooseAction(params: Params) {
        if (TextCommands.CATEGORY_CHOOSE_ACTION.isAllowed(params.userActualizedInfo)) {
            sendMessage(
                params,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        } else {
            params.userActualizedInfo =
                params.userActualizedInfo.copy(
                    lastUserActionType = LastUserActionType.DEFAULT,
                )
            sendMessage(
                params,
                "🔒 Действие недоступно для вас",
            )
        }
    }

    private fun sendMessage(
        params: Params,
        text: String,
    ) {
        val sendMessage =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = text,
                ),
            )
        params.userActualizedInfo.data?.categoryMessageId = sendMessage.messageId

    }

    private fun sendMessage(
        params: Params,
        text: String,
        keyboard: InlineKeyboardMarkup,
    ) {
        val sendMessage =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = text,
                    replyMarkup = keyboard,
                ),
            )
        params.userActualizedInfo.data?.categoryMessageId = sendMessage.messageId
    }
}
