package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
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
        var userInfo: UserActualizedInfo,
    )
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo{
        if (update.callbackQuery==null) return userActualizedInfo
        val callbackData = update.callbackQuery.data
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData = RequestData(
            chatId,
            update,
            userActualizedInfo,
        )
        bot.execute(AnswerCallbackQuery(update.callbackQuery.id))
        when {
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
            CallbackCommands.CATEGORY_CHOOSE.isMatch(callbackData) ->
                clickChoose(requestData,CallbackCommands.params(callbackData))
            CallbackCommands.CATEGORY_CONFIRM.isMatch(callbackData) ->
                clickConfirm(requestData,CallbackCommands.params(callbackData))
        }
        return requestData.userInfo
    }
    private fun clickActionMenu(data: RequestData){
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_ACTION_CHOOSING
        )
        editMessage(
            data,
            "⬇️ Выберите действие",
            CategoryKeyboards.choosingAction()
        )
    }
    private fun clickEdit(data: RequestData){
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_EDITING
        )
        editMessage(
            data,
            "✏️ Выберите категорию для изменения",
            CategoryKeyboards.choosingCategory(
                0L,6,categoryRepository
            )
        )
    }
    private fun clickAdd(data: RequestData){
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_ADDING
        )
        //TODO: Реализовать добавление
    }
    private fun clickDelete(data: RequestData){
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_DELETING
        )
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
    private fun clickChoose(data: RequestData, params: List<String>){
        val catId = params[0].toLong()
        val category = categoryRepository.findById(catId)
        when(data.userInfo.lastUserActionType){
            LastUserActionType.CATEGORY_DELETING ->
                editMessage(
                    data,
                    "❓ Вы действительно хотите удалить категорию с названием:\n" +
                            "${category.get().title}\n" +
                            "тэгом: ${category.get().suffix}\n" +
                            "описанием: ${category.get().description}\n",
                    CategoryKeyboards.confirmationAction(catId)
                )
            LastUserActionType.CATEGORY_EDITING ->
                editMessage(
                    data,
                    "❓ Вы действительно хотите изменить категорию с названием:\n" +
                            "${category.get().title}\n" +
                            "тэгом: ${category.get().suffix}\n" +
                            "описанием: ${category.get().description}\n",
                    CategoryKeyboards.confirmationAction(catId)
                )
            else -> return
        }
    }
    private fun clickConfirm(data: RequestData, params: List<String>){
        val catId = params[0].toLong()
        val category = categoryRepository.findById(catId)
        when(data.userInfo.lastUserActionType){
            LastUserActionType.CATEGORY_DELETING -> {
                categoryRepository.deleteById(catId)
                editMessage(
                    data,
                    "✅ Категория #${category.get().suffix} успешно удалена",
                    CategoryKeyboards.confirmationDone()
                )
            }
            LastUserActionType.CATEGORY_EDITING -> return
            else -> return
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