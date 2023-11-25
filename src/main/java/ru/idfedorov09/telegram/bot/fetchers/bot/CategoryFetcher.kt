package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.w3c.dom.Text
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.RedisService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class CategoryFetcher (
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
    private val redisService: RedisService,
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
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update)!!.lowercase()
        val user = updatesUtil.getUser(update) ?: return
        if(TextCommands.isTextCommand(messageText)){
            if(messageText == TextCommands.CATEGORY_CHOOSE_ACTION.commandText){
                val msg = SendMessage()
                msg.chatId=chatId
                msg.text="aboba"
                msg.replyMarkup = createCategoryActionChoiceKeyboard()
                bot.execute(msg)
            }
        }
    }

    private fun createCategoryActionChoiceKeyboard(): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = listOf(
            listOf(
                InlineKeyboardButton("Изменить ✏️").also {
                    it.callbackData = TextCommands.CATEGORY_ADD.commandText },
                InlineKeyboardButton("Добавить ✅").also {
                    it.callbackData = TextCommands.CATEGORY_ADD.commandText },
                InlineKeyboardButton("Удалить ❌").also {
                    it.callbackData = TextCommands.CATEGORY_DELETE.commandText  },
            ),
        )
        return keyboard
    }
}