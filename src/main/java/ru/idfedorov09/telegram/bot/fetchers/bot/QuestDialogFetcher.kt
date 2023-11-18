
package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
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
        userActualizedInfo: UserActualizedInfo?,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        userActualizedInfo ?: return

        // если апдейт из беседы, то игнорим
        if (update.message.chatId.toString() != userActualizedInfo.tui) return

        val messageText = update.message.text
        val quest = Quest(
            authorId = update.message.chatId,
            questionStatus = QuestionStatus.WAIT,
        ).let { questRepository.save(it) }

        val questDialogMessage = QuestDialogMessage(
            questId = quest.id,
            isByQuestionAuthor = true,
            authorId = userActualizedInfo.id,
            messageText = messageText,
        ).let { questDialogMessageRepository.save(it) }

        quest.dialogHistory.add(questDialogMessage.id!!)
        questRepository.save(quest)

        // TODO: обработать случай когда у юзера нет никнейма
        bot.execute(
            SendMessage().also {
                it.chatId = RESPONDENT_CHAT_ID
                it.text =
                    "Получен вопрос #${quest.id} от @${userActualizedInfo.lastTgNick} (${userActualizedInfo.fullName})"
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
                it.text = "Выберите действие:"
                it.replyMarkup = createChooseKeyboard(quest)
            },
        )
    }
    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(quest: Quest) = createKeyboard(
        listOf(
            listOf(InlineKeyboardButton("\uD83D\uDCAC Ответ").also { it.callbackData = "ans ${quest.id}" }),
            listOf(
                InlineKeyboardButton("\uD83D\uDD07 Игнор").also { it.callbackData = "ignore ${quest.id}" },
                InlineKeyboardButton("\uD83D\uDEAF Бан").also { it.callbackData = "ban ${quest.id}" },
            ),
        ),
    )
}
