package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.CategoryStage
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
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
    var lastSentMessage: Message? = null
    private data class RequestData(
        val chatId: String,
        val update: Update,
        val exp: ExpContainer,
        val userInfo: UserActualizedInfo,
    )
    @InjectData
    fun doFetch(
        update: Update,
        expContainer: ExpContainer,
        userActualizedInfo: UserActualizedInfo,
    ){
        if(update.message == null || !update.message.hasText())return
        val chatId = updatesUtil.getChatId(update) ?: return
        val requestData = RequestData(
            chatId,
            update,
            expContainer,
            userActualizedInfo,
        )
        val messageText = update.message.text
        return when {
            messageText==TextCommands.CATEGORY_CHOOSE_ACTION.commandText ->
                commandChoseAction(requestData)
            else -> return
        }
    }
    private fun commandChoseAction(data: RequestData){
        if(TextCommands.CATEGORY_CHOOSE_ACTION.isAllowed(data.userInfo)){
            data.exp.categoryStage = CategoryStage.ACTION_CHOOSING
            sendMessage(data,"⬇️ Выберите действие",createChoosingActionKeyboard())
        }else {
            data.exp.categoryStage = CategoryStage.WAITING
            sendMessage(data,"🔒 Действие недоступно для вас")
        }
    }
    private fun createChoosingActionKeyboard(): InlineKeyboardMarkup{
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("✏️ Изменить").also {
                        it.callbackData = CallbackCommands.CATEGORY_EDIT.data
                    },
                    InlineKeyboardButton("✅ Добавить").also {
                        it.callbackData = CallbackCommands.CATEGORY_ADD.data
                    },
                    InlineKeyboardButton("❌ Удалить").also {
                        it.callbackData = CallbackCommands.CATEGORY_DELETE.data
                    },
                ),
            )
        )
    }
    private fun sendMessage(data: RequestData, text: String){
        lastSentMessage=bot.execute(SendMessage(data.chatId,text))
    }
    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup){
        val msg = SendMessage(data.chatId,text)
        msg.replyMarkup=keyboard
        lastSentMessage=bot.execute(msg)
    }
    private fun editMessage(msg: Message, data: RequestData, text: String){
        bot.execute(
            EditMessageText(
                data.chatId,
                msg.messageId,
                null,
                text,
                null,
                null,
                null,
                null,
            )
        )
    }
    private fun editMessage(msg: Message, data: RequestData, keyboard: InlineKeyboardMarkup){
        bot.execute(
            EditMessageReplyMarkup(
                data.chatId,
                msg.messageId,
                null,
                keyboard,
            )
        )
    }
}