package ru.idfedorov09.telegram.bot.fetchers.bot

import kotlinx.coroutines.flow.callbackFlow
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.CategoryStage
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
        if (update.callbackQuery==null) return
        val callbackData = update.callbackQuery.data
        val chatId = updatesUtil.getChatId(update) ?: return
        val requestData = RequestData(
            chatId,
            update,
            expContainer,
            userActualizedInfo,
        )
        return when {
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
    private fun clickEdit(data: RequestData){
        data.exp.categoryStage = CategoryStage.EDITING
        sendMessage(data,"✏️ Выберите категорию для изменения",createChoosingKeyboard(1L))
    }
    private fun clickAdd(data: RequestData){
        data.exp.categoryStage = CategoryStage.ADDING
        //TODO: Реализовать добавление
    }
    private fun clickDelete(data: RequestData){
        data.exp.categoryStage = CategoryStage.DELETING
        sendMessage(data,"❌ Выберите категорию для удаления",createChoosingKeyboard(1L))
    }
    private fun clickPage(data: RequestData, params: List<String>){
        val msg = lastSentMessage ?: return
        editMessage(msg,data,createChoosingKeyboard(params[0].toLong()))
    }
    private fun createChoosingKeyboard(page: Long): InlineKeyboardMarkup{
        val body=createChoosingKeyboardBody(page)
        val nav=createChoosingKeyboardNav(page)
        body.addAll(nav)
        return InlineKeyboardMarkup(body)
    }
    private fun createChoosingKeyboardBody(page: Long): MutableList<MutableList<InlineKeyboardButton>>{
        return mutableListOf(mutableListOf())
    }
    private fun createChoosingKeyboardNav(page: Long): MutableList<MutableList<InlineKeyboardButton>>{
        val count=categoryRepository.count()/6+1
        return mutableListOf(
            mutableListOf(
                InlineKeyboardButton("⬅️ Назад").also {
                    it.callbackData = CallbackCommands.CATEGORY_PAGE.format(realMod(page-1,count))
                },
                InlineKeyboardButton("$page|$count").also {
                    it.callbackData = CallbackCommands.VOID.data
                },
                InlineKeyboardButton("Вперёд ➡️").also {
                    it.callbackData = CallbackCommands.CATEGORY_PAGE.format(realMod(page+1,count))
                },
            )
        )
    }
    private fun realMod(a: Long, b: Long): Long{
        if(a in 1..b){
            return a
        }
        if(a<=0){
            return (a-1)%b+1
        }
        if(a>b){
            return a%b
        }
        return 0
    }
    private fun sendMessage(data: RequestData, text: String){
        lastSentMessage=bot.execute(SendMessage(data.chatId,text))
    }
    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup){
        var msg = SendMessage(data.chatId,text)
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