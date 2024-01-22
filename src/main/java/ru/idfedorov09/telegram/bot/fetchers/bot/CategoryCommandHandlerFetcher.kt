package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий комманды категорий
 */
@Component
class CategoryCommandHandlerFetcher(
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {
    private data class RequestData(
        val chatId: String,
        val update: Update,
        var userInfo: UserActualizedInfo,
    )

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (update.message == null || !update.message.hasText()) return userActualizedInfo
        val messageText = update.message.text
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData = RequestData(
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
            data.userInfo = data.userInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_ACTION_CHOOSING,
            )
            sendMessage(
                data,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        } else {
            data.userInfo = data.userInfo.copy(
                lastUserActionType = LastUserActionType.DEFAULT,
            )
            sendMessage(
                data,
                "🔒 Действие недоступно для вас",
            )
        }
    }

    private fun sendMessage(data: RequestData, text: String) {
        val lastSent = bot.execute(SendMessage(data.chatId, text)).messageId
        data.userInfo = data.userInfo.copy(
            data = lastSent.toString(),
        )
    }

    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup) {
        val msg = SendMessage(data.chatId, text)
        msg.replyMarkup = keyboard
        val lastSent = bot.execute(msg).messageId
        data.userInfo = data.userInfo.copy(
            data = lastSent.toString(),
        )
    }
}
