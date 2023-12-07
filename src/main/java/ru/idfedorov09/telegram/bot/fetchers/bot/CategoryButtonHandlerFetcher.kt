package ru.idfedorov09.telegram.bot.fetchers.bot

import kotlinx.coroutines.flow.callbackFlow
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.CategoryStage
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий случаи нажатия на кнопки для действий с категориями
 */
@Component
class CategoryButtonHandlerFetcher (
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {
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
        if (update.callbackQuery==null) return
        val callbackData = update.callbackQuery.data
        val chatId = updatesUtil.getChatId(update) ?: return
        val requestData = RequestData(
            chatId,
            update,
            expContainer,
            userActualizedInfo,
        )
        bot.execute(AnswerCallbackQuery(update.callbackQuery.id))
        return when {
            CallbackCommands.CATEGORY_ACTION_MENU.isMatch(callbackData) ->
                clickActionMenu(requestData)
            CallbackCommands.CATEGORY_EDIT.isMatch(callbackData) ->
                clickEdit(requestData)
            CallbackCommands.CATEGORY_ADD.isMatch(callbackData) ->
                clickAdd(requestData)
            CallbackCommands.CATEGORY_DELETE.isMatch(callbackData) ->
                clickDelete(requestData)
            CallbackCommands.CATEGORY_PAGE.isMatch(callbackData) ->
                clickPage(requestData,CallbackCommands.params(callbackData))
            else -> return
        }
    }
    private fun clickActionMenu(data: RequestData){
        data.exp.categoryStage = CategoryStage.ACTION_CHOOSING
        editMessage(
            data,
            "⬇️ Выберите действие",
            CategoryKeyboards.choosingAction()
        )
    }
    private fun clickEdit(data: RequestData){
        data.exp.categoryStage = CategoryStage.EDITING
        editMessage(
            data,
            "✏️ Выберите категорию для изменения",
            CategoryKeyboards.choosingCategory(
                0L,6,categoryRepository
            )
        )
    }
    private fun clickAdd(data: RequestData){
        data.exp.categoryStage = CategoryStage.ADDING
        //TODO: Реализовать добавление
    }
    private fun clickDelete(data: RequestData){
        data.exp.categoryStage = CategoryStage.DELETING
        editMessage(
            data,
            "❌ Выберите категорию для удаления",
            CategoryKeyboards.choosingCategory(
                0L,6,categoryRepository
            )
        )
    }
    private fun clickPage(data: RequestData, params: List<String>){
        editMessage(
            data,
            CategoryKeyboards.choosingCategory(
                params[0].toLong(),6,categoryRepository
            )
        )
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