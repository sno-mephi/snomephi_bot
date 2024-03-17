package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_ANSWER
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_BAN
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_IGNORE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_START_DIALOG
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import java.lang.NumberFormatException
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер, обрабатывающий случаи нажатия на кнопки для вопросов
 */
@Component
class QuestButtonHandlerFetcher(
    private val bot: Executor,
    private val messageSenderService: MessageSenderService,
    private val questRepository: QuestRepository,
    private val userRepository: UserRepository,
    private val dialogMessageRepository: QuestDialogMessageRepository,
    private val switchKeyboardService: SwitchKeyboardService,
) : DefaultFetcher() {
    // TODO: обработать случай когда бот не может написать пользователю!
    // TODO: нельзя отвечать самому себе
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (!update.hasCallbackQuery()) return userActualizedInfo
        val callbackData = update.callbackQuery.data
        if (!Regex("^.*\\|\\d+$").matches(callbackData) ||
            !userActualizedInfo.isRegistered
        ) {
            return userActualizedInfo
        }

        val questByCallbackData = getQuestByCallbackData(callbackData) ?: return userActualizedInfo

        val requestData =
            RequestData(
                questByCallbackData,
                userActualizedInfo,
                update,
            )
        bot.execute(AnswerCallbackQuery(update.callbackQuery.id))
        return when {
            QUEST_ANSWER.isMatch(callbackData) -> clickAnswer(requestData)
            QUEST_IGNORE.isMatch(callbackData) -> clickIgnore(requestData)
            QUEST_BAN.isMatch(callbackData) -> clickBan(requestData)
            QUEST_START_DIALOG.isMatch(callbackData) -> clickStartDialog(requestData)
            else -> userActualizedInfo
        }
    }

    private fun clickStartDialog(data: RequestData): UserActualizedInfo {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return data.userActualizedInfo
        if (data.userActualizedInfo.activeQuest != null) return data.userActualizedInfo

        val quest =
            data.quest.copy(
                responderId = data.userActualizedInfo.id,
                questionStatus = QuestionStatus.DIALOG,
            )

        val questionAuthor = userRepository.findActiveUsersById(data.quest.authorId!!)!!
        questRepository.save(quest)
        userRepository.save(
            questionAuthor.copy(
                questDialogId = data.quest.id,
            ),
        )

        switchKeyboardService.disableKeyboard(questionAuthor.id!!)
        switchKeyboardService.switchKeyboard(
            data.userActualizedInfo.id!!,
            UserKeyboardType.DIALOG_QUEST,
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = questionAuthor.tui!!,
                text = "<i>С вами общается оператор по поводу обращения #${data.quest.id}</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text =
                    "<i>Ты перешел в диалог с пользователем @${questionAuthor.lastTgNick}. " +
                        "Несмотря на твою анонимность, оставайся вежливым :)</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = quest.consoleMessageId!!.toInt(),
                text = "✏\uFE0F ${data.userActualizedInfo.lastTgNick} ведет диалог",
            ),
        )

        return data.userActualizedInfo.copy(
            activeQuest = data.quest,
        )
    }

    private fun clickIgnore(data: RequestData): UserActualizedInfo {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return data.userActualizedInfo

        questRepository.save(
            data.quest.copy(
                questionStatus = QuestionStatus.CLOSED,
            ),
        )

        // TODO: а если у пользователя нет ника?
        val newText = "\uD83D\uDFE1 Проигнорировано пользователем @${data.userActualizedInfo.lastTgNick}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = data.quest.consoleMessageId?.toInt(),
                text = newText,
            ),
        )
        return data.userActualizedInfo
    }

    private fun clickAnswer(data: RequestData): UserActualizedInfo {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return data.userActualizedInfo
        if (data.userActualizedInfo.activeQuest != null) return data.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(data.quest.authorId!!)!!
        val firstMessage = dialogMessageRepository.findById(data.quest.dialogHistory.first()).get()

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text = "Кажется, ты хотел(-а) ответить на следующее сообщение:",
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                fromChatId = questionAuthor.tui.toString(),
                messageId = firstMessage.messageId!!,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = data.userActualizedInfo.tui,
                text =
                    "Ты можешь либо ответить анонимно одним сообщением, отправив его сейчас, " +
                        "либо начать анонимный диалог с пользователем.",
                replyMarkup = createChooseKeyboard(data.quest),
            ),
        )

        return data.userActualizedInfo.copy(
            lastUserActionType = LastUserActionType.ACT_QUEST_ANS_CLICK,
        )
    }

    private fun clickBan(data: RequestData): UserActualizedInfo {
        if (data.quest.questionStatus == QuestionStatus.CLOSED) return data.userActualizedInfo
        questRepository.save(
            data.quest.copy(
                questionStatus = QuestionStatus.CLOSED,
            ),
        )

        // TODO: логика банов скоро изменится, тут тоже надо будет менять код
        val authorInBan =
            userRepository.findActiveUsersById(data.quest.authorId!!)!!.copy(
                roles = mutableSetOf(UserRole.BANNED),
            )
        userRepository.save(authorInBan)

        // TODO: а если у пользователя нет ника?
        val newText = "\uD83D\uDD34 Автор забанен пользователем @${data.userActualizedInfo.lastTgNick}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = data.quest.consoleMessageId?.toInt(),
                text = newText,
                replyMarkup = createUnbanKeyboard(data.quest),
            ),
        )

        return data.userActualizedInfo
    }

    private fun createChooseKeyboard(quest: Quest) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Начать диалог")
                        .also { it.callbackData = QUEST_START_DIALOG.format(quest.id) },
                ),
            ),
        )

    private fun createUnbanKeyboard(quest: Quest) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("Разбанить (doesn't work)")
                        // TODO("разбан еще не реализован")
                        .also { it.callbackData = QUEST_BAN.format(quest.id) },
                ),
            ),
        )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun getQuestByCallbackData(callbackData: String): Quest? {
        val questId = parseQuestId(callbackData)
        return questRepository.findById(questId).getOrNull()
    }

    private fun parseQuestId(callbackData: String): Long {
        val questId =
            try {
                callbackData.split("|")[1].toLong()
            } catch (e: NumberFormatException) {
                throw NumberFormatException(
                    "Error during parse callBackData in questButtonHandler fetcher. " +
                        "Callback data: '$callbackData' has incorrect format. Correct format: 'something|{LONG}'",
                )
            }
        return questId
    }

    private data class RequestData(
        val quest: Quest,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
