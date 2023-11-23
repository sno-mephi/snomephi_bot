package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import java.lang.NumberFormatException

/**
 * Фетчер, обрабатывающий случаи нажатия на кнопки для вопросов
 */
@Component
class QuestButtonHandlerFetcher(
    private val bot: Executor,
    private val questRepository: QuestRepository,
    private val userRepository: UserRepository,
    private val dialogMessageRepository: QuestDialogMessageRepository,
) : GeneralFetcher() {

    // TODO: обработать случай когда бот не может написать пользователю!
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        if (!update.hasCallbackQuery()) return

        val requestData = RequestData(
            getQuestByCallbackData(update.callbackQuery.data),
            userActualizedInfo,
            update,
        )

        with(update.callbackQuery.data) {
            when {
                startsWith("quest_ans") -> clickAnswer(requestData)
                startsWith("quest_ignore") -> clickIgnore(requestData)
                startsWith("quest_ban") -> clickBan(requestData)
            }
        }
    }

    private fun getQuestByCallbackData(callbackData: String): Quest {
        val questId = parseQuestId(callbackData)
        return questRepository.findById(questId).get()
    }

    private fun parseQuestId(callbackData: String): Long {
        val questId =
            try {
                callbackData.split(" ")[1].toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException(
                    "Error during parse callBackData in questButtonHandler fetcher. " +
                        "Callback data: $callbackData) has incorrect format. Correct format: 'something {LONG}'",
                )
            }
        return questId
    }

    private fun clickIgnore(data: RequestData) {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return

        questRepository.save(
            data.quest.copy(
                questionStatus = QuestionStatus.CLOSED,
            ),
        )

        // TODO: а если у пользователя нет ника?
        val newText = "\uD83D\uDFE1 Ответ проигнорирован пользователем @${data.userActualizedInfo.lastTgNick}."
        bot.execute(
            EditMessageText().also {
                it.chatId = QUEST_RESPONDENT_CHAT_ID
                it.messageId = data.quest.consoleMessageId?.toInt()
                it.text = newText
            },
        )
    }

    private fun clickAnswer(data: RequestData) {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return
        val messageText = "Вы можете либо ответить одним сообщением, отправив его сейчас, " +
                "либо начать анонимный диалог с пользователем."
        val questionAuthor = userRepository.findById(data.quest.authorId!!).get()
        val firstMessage = dialogMessageRepository.findById(data.quest.dialogHistory.first()).get()

        bot.execute(
            SendMessage().also {
                it.chatId = data.userActualizedInfo.tui
                it.text = "Кажется, вы хотели ответить на следующее сообщение:"
            }
        )
        bot.execute(
            ForwardMessage().also {
                it.chatId = data.userActualizedInfo.tui
                it.fromChatId = questionAuthor.tui.toString()
                it.messageId = firstMessage.messageId!!
            },
        )
        bot.execute(
            SendMessage().also {
                it.chatId = data.userActualizedInfo.tui
                it.text = messageText
                it.replyMarkup = createChooseKeyboard(data.quest)
            }
        )
    }

    private fun clickBan(data: RequestData) {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return
        TODO()
    }

    private fun createChooseKeyboard(quest: Quest) = createKeyboard(
        listOf(
            listOf(InlineKeyboardButton("\uD83D\uDCAC Начать диалог").also { it.callbackData = "quest_start_dialog ${quest.id}" }),
        ),
    )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private data class RequestData(
        val quest: Quest,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
