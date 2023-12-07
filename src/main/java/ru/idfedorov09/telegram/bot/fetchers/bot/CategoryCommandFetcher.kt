package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, отправляющий inline клавиатуру выбора действия с категориями
 */
@Component
class CategoryCommandFetcher (
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val categoryRepository: CategoryRepository,
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
    ): UserActualizedInfo{
        if(update.message == null || !update.message.hasText())return userActualizedInfo
        val messageText = update.message.text
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData = RequestData(
            chatId,
            update,
            userActualizedInfo,
        )
        when {
            messageText==TextCommands.CATEGORY_CHOOSE_ACTION.commandText ->
                commandChoseAction(requestData)
        }
        return requestData.userInfo
    }
    private fun commandChoseAction(data: RequestData){
        if(TextCommands.CATEGORY_CHOOSE_ACTION.isAllowed(data.userInfo)){
            data.userInfo = data.userInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_ACTION_CHOOSING
            )
            sendMessage(
                data,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction()
            )
        }else {
            data.userInfo = data.userInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_WAITING
            )
            sendMessage(
                data,
                "🔒 Действие недоступно для вас"
            )
        }
    }
    private fun sendMessage(data: RequestData, text: String){
        bot.execute(SendMessage(data.chatId,text)).messageId
    }
    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup){
        val msg = SendMessage(data.chatId,text)
        msg.replyMarkup=keyboard
        bot.execute(msg).messageId
    }
    private fun editMessage(data: RequestData, text: String){
        val msgId = data.update.callbackQuery.message.messageId
        bot.execute(
            EditMessageText(
                data.chatId,
                msgId,
                null,
                text,
                null,
                null,
                null,
                null,
            )
        )
    }
    private fun editMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup){
        val msgId = data.update.callbackQuery.message.messageId
        bot.execute(
            EditMessageText(
                data.chatId,
                msgId,
                null,
                text,
                null,
                null,
                keyboard,
                null,
            )
        )
    }
    private fun editMessage(data: RequestData, keyboard: InlineKeyboardMarkup){
        val msgId = data.update.callbackQuery.message.messageId
        bot.execute(
            EditMessageReplyMarkup(
                data.chatId,
                msgId,
                null,
                keyboard,
            )
        )
    }
}