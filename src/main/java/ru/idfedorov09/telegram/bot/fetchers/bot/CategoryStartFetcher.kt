package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.CategoryStage
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, отправляющий inline клавиатуру выбора действия с категориями
 */
@Component
class CategoryStartFetcher (
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ActualizeUserInfoFetcher::class.java)
    }
    @InjectData
    fun doFetch(
        update: Update,
        expContainer: ExpContainer,
        userActualizedInfo: UserActualizedInfo,
    ){
        if(update.message == null || !update.message.hasText())return
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = update.message.text ?: return
        if(messageText == TextCommands.CATEGORY_CHOOSE_ACTION.commandText){
            if(TextCommands.CATEGORY_CHOOSE_ACTION.isAllowed(userActualizedInfo)){
                expContainer.categoryStage = CategoryStage.ACTION_CHOOSING
                val msg = SendMessage()
                msg.chatId = chatId
                msg.text = "⬇️ Выберите действие"
                msg.replyMarkup = createActionChoosingKeyboard()
                bot.execute(msg)
            }else {
                val msg = SendMessage()
                msg.chatId = chatId
                msg.text = "🔒 Действие недоступно для вас"
                bot.execute(msg)
            }
        }else{
            return
        }
    }
    private fun createActionChoosingKeyboard() = InlineKeyboardMarkup(
        listOf(
            listOf(
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