package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
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
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        expContainer: ExpContainer,
        userActualizedInfo: UserActualizedInfo,
    ){
        if (update.callbackQuery==null) return
        val callbackData = update.callbackQuery.data
        val requestData = CategoryButtonHandlerFetcher.RequestData(
            update,
            expContainer,
            userActualizedInfo,
        )
        return when {
            CallbackCommands.CATEGORY_EDIT.isMatch(callbackData) -> clickEdit(requestData)
            CallbackCommands.CATEGORY_ADD.isMatch(callbackData) -> clickAdd(requestData)
            CallbackCommands.CATEGORY_DELETE.isMatch(callbackData) -> clickDelete(requestData)
            else -> return
        }
    }
    private fun clickEdit(data: RequestData){
        data.expContainer.categoryStage = CategoryStage.ACTION_CHOOSING
        val msg = SendMessage()
        msg.chatId = updatesUtil.getChatId(data.update) ?: return
        msg.text = "⬇️ Выберите действие"
        msg.replyMarkup = createEditingKeyboard()
        bot.execute(msg)
    }
    private fun clickAdd(data: RequestData){
        data.expContainer.categoryStage = CategoryStage.ACTION_CHOOSING
        val msg = SendMessage()
        msg.chatId = updatesUtil.getChatId(data.update) ?: return
        msg.text = "⬇️ Выберите действие"
        msg.replyMarkup = createAddingKeyboard()
        bot.execute(msg)
    }
    private fun clickDelete(data: RequestData){
        data.expContainer.categoryStage = CategoryStage.ACTION_CHOOSING
        val msg = SendMessage()
        msg.chatId = updatesUtil.getChatId(data.update) ?: return
        msg.text = "⬇️ Выберите действие"
        msg.replyMarkup = createDeletingKeyboard()
        bot.execute(msg)
    }
    private data class RequestData(
        val update: Update,
        val expContainer: ExpContainer,
        val userActualizedInfo: UserActualizedInfo,
    )
    private fun createEditingKeyboard() = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("✏️ Изменить").also {
                    it.callbackData = CallbackCommands.CATEGORY_EDIT.data
                },
            ),
        )
    )
    private fun createAddingKeyboard() = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("✅ Добавить").also {
                    it.callbackData = CallbackCommands.CATEGORY_ADD.data
                },
            ),
        )
    )
    private fun createDeletingKeyboard() = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("❌ Удалить").also {
                    it.callbackData = CallbackCommands.CATEGORY_DELETE.data
                },
            ),
        )
    )
}