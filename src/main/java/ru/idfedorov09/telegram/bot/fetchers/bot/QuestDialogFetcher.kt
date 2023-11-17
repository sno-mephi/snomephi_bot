
package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class QuestDialogFetcher(
    private val updatesUtil: UpdatesUtil,
    private val questRepository: QuestRepository,
    private val questDialogMessageRepository: QuestDialogMessageRepository,
) : GeneralFetcher() {
    companion object {
        private const val RESPONDENT_CHAT_ID = "-1002057270905" // [ЛОГИ] чат долбаебов
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val text = update.message.text
        if ((userActualizedInfo.questDialog == null) or TextCommands.isTextCommand(text)) {
            return
        }
        val quest = Quest(
            authorId = update.message.chatId,
            questionStatus = QuestionStatus.WAIT,
        )
        val questDialogId = questRepository.save(quest).id
        val questDialogMessage = QuestDialogMessage(
            questId = questDialogId,
            isByQuestionAuthor = true,
            authorId = update.message.chatId,
            messageText = text,
        )
        val questDialogMessageId = questDialogMessageRepository.save(questDialogMessage).id
        userActualizedInfo.questDialog?.dialogHistory?.add(questDialogMessageId!!)

        bot.execute(
            SendMessage().also {
                it.chatId = RESPONDENT_CHAT_ID
                it.text = "Получен вопрос № $questDialogId от ${userActualizedInfo.fullName} " +
                    userActualizedInfo.lastTgNick
            },
        )
        bot.execute(
            ForwardMessage().also {
                it.chatId = RESPONDENT_CHAT_ID
                it.fromChatId = updatesUtil.getChatId(update).toString()
                it.messageId = update.message.messageId
            },
        )
        bot.execute(
            SendMessage().also {
                it.chatId = RESPONDENT_CHAT_ID
                it.text = "Выберите действие"
                it.replyMarkup = createChooseKeyboard()
            },
        )
    }
    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }
    private fun createChooseKeyboard() = createKeyboard(
        listOf(
            mutableListOf(
                InlineKeyboardButton("ответ ✅").also { it.callbackData = "start" },
                InlineKeyboardButton("игнор ✅").also { it.callbackData = "ignore" },
                InlineKeyboardButton("бан ❌").also { it.callbackData = "ban" },
            ),
        ),
    )
}
