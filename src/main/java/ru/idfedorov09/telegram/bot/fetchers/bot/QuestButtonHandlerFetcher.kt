package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUEST_RESPONDENT_CHAT_ID
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_ANSWER
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_RECREATE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_RECREATE_START_DIALOG
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_BAN
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_IGNORE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_START_DIALOG
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_SHOW_HISTORY
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.QuestSegment
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.QuestMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.lang.NumberFormatException
import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull
import ru.idfedorov09.telegram.bot.data.enums.*

/**
 * Фетчер, обрабатывающий случаи нажатия на кнопки для вопросов
 */
@Component
class QuestButtonHandlerFetcher(
    private val updatesUtil: UpdatesUtil,
    private val bot: Executor,
    private val messageSenderService: MessageSenderService,
    private val questDialogRepository: QuestDialogRepository,
    private val questSegmentRepository: QuestSegmentRepository,
    private val userRepository: UserRepository,
    private val questMessageRepository: QuestMessageRepository,
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
        val segment = questByCallbackData.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() }

        val params =
            Params(
                questByCallbackData,
                segment!!,
                userActualizedInfo,
                update,
            )
        return when {
                QUEST_ANSWER.isMatch(callbackData) -> clickAnswer(params)
                QUEST_IGNORE.isMatch(callbackData) -> clickIgnore(params)
                QUEST_BAN.isMatch(callbackData) -> clickBan(params)
                QUEST_START_DIALOG.isMatch(callbackData) -> clickStartDialog(params)
                QUEST_RECREATE.isMatch(callbackData) -> clickRecreate(params)
                QUEST_RECREATE_START_DIALOG.isMatch(callbackData) -> clickRecreateStartDialog(params)
                else -> userActualizedInfo
            }
        }

    private fun clickStartDialog(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus != QuestionStatus.WAIT){
            val callbackAnswer = AnswerCallbackQuery().also{
                it.text = "\uD83D\uDC40 Возможно, на этот вопрос уже ответили или отвечают"
                it.callbackQueryId = params.update.callbackQuery.id
            }
            bot.execute(callbackAnswer)
            return params.userActualizedInfo
        }
        if (params.userActualizedInfo.activeQuestDialog != null) return params.userActualizedInfo

        val quest =
            params.questDialog.copy(
                questionStatus = QuestionStatus.DIALOG,
            )

        questSegmentRepository.save(
            params.questSegment.copy(
                responderId = params.userActualizedInfo.id,
            )
        )

        val questionAuthor = userRepository.findActiveUsersById(params.questDialog.authorId!!)!!
        questDialogRepository.save(quest)
        userRepository.save(
            questionAuthor.copy(
                questDialogId = params.questDialog.id,
            ),
        )

        switchKeyboardService.switchKeyboard(
            questionAuthor.id!!,
            UserKeyboardType.DIALOG_QUEST,
        )
        switchKeyboardService.switchKeyboard(
            params.userActualizedInfo.id!!,
            UserKeyboardType.DIALOG_QUEST,
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = questionAuthor.tui!!,
                text = "<i>С вами общается оператор по поводу обращения #${params.questDialog.id}</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text =
                    "<i>Ты перешел в диалог с пользователем ${MessageSenderUtil.userName(
                        questionAuthor.lastTgNick,
                        questionAuthor.fullName,
                    )}. " +
                        "Несмотря на твою анонимность, оставайся вежливым :)</i>",
                parseMode = ParseMode.HTML,
            ),
        )

        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = quest.consoleMessageId!!.toInt(),
                text =
                    "✏\uFE0F ${MessageSenderUtil.userName(params.userActualizedInfo.lastTgNick, params.userActualizedInfo.fullName)} " +
                        "ведет диалог",
            ),
        )

        return params.userActualizedInfo.copy(
            activeQuestDialog = params.questDialog,
        )
    }

    private fun clickIgnore(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus != QuestionStatus.WAIT) return params.userActualizedInfo

        questDialogRepository.save(
            params.questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            ),
        )

        questSegmentRepository.save(
            params.questSegment.copy(
                responderId = params.userActualizedInfo.id,
                finishTime = updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            )
        )

        val newText = "\uD83D\uDFE1 Проигнорировано пользователем ${MessageSenderUtil.userName(
            params.userActualizedInfo.lastTgNick,
            params.userActualizedInfo.fullName,
        )}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = params.questDialog.consoleMessageId?.toInt(),
                text = newText,
                replyMarkup = createRecreateKeyboard(params.questDialog)
            ),
        )
        return params.userActualizedInfo
    }

    private fun clickAnswer(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus != QuestionStatus.WAIT) return params.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(params.questDialog.authorId!!)!!

        if (params.userActualizedInfo.tui == questionAuthor.tui){

            val answerCallbackQuery = AnswerCallbackQuery().also {
                it.callbackQueryId = params.update.callbackQuery.id
                it.text = "Вы не можете отвечать самому себе!"
                it.showAlert = true
            }
            bot.execute(answerCallbackQuery)

            return params.userActualizedInfo
        }

        if (params.userActualizedInfo.activeQuestDialog != null) return params.userActualizedInfo
        val firstMessage = questMessageRepository.findById(params.questDialog.dialogHistory.first()).get()

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Кажется, ты хотел(-а) ответить на следующее сообщение:",
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                fromChatId = questionAuthor.tui.toString(),
                messageId = firstMessage.messageId!!,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Ты можешь начать анонимный диалог с пользователем:",
                replyMarkup = createChooseKeyboard(params.questDialog),
            ),
        )

        return params.userActualizedInfo
    }

    private fun clickBan(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus == QuestionStatus.CLOSED) return params.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(params.questDialog.authorId!!)!!

        if (params.userActualizedInfo.tui == questionAuthor.tui){

            val answerCallbackQuery = AnswerCallbackQuery().also {
                it.callbackQueryId = params.update.callbackQuery.id
                it.text = "\uD83D\uDEAB Вы не можете забанить себя"
            }
            bot.execute(answerCallbackQuery)

            return params.userActualizedInfo
        }

        questDialogRepository.save(
            params.questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            ),
        )

        questSegmentRepository.save(
            params.questSegment.copy(
                responderId = params.userActualizedInfo.id,
                finishTime = updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            )
        )

        // TODO: логика банов скоро изменится, тут тоже надо будет менять код
        val authorInBan =
            userRepository.findActiveUsersById(params.questDialog.authorId)!!.copy(
                roles = mutableSetOf(UserRole.BANNED),
            )
        userRepository.save(authorInBan)

        val newText = "\uD83D\uDD34 Автор забанен пользователем ${MessageSenderUtil.userName(
            params.userActualizedInfo.lastTgNick,
            params.userActualizedInfo.fullName,
        )}."
        messageSenderService.editMessage(
            MessageParams(
                chatId = QUEST_RESPONDENT_CHAT_ID,
                messageId = params.questDialog.consoleMessageId?.toInt(),
                text = newText,
                replyMarkup = createUnbanKeyboard(params.questDialog),
            ),
        )

        return params.userActualizedInfo
    }

    private fun clickRecreate(params: Params): UserActualizedInfo {
        params.apply {
            if (questDialog.questionStatus != QuestionStatus.IGNORE &&
                questDialog.questionStatus != QuestionStatus.CLOSED) return userActualizedInfo

            val questionAuthor = userRepository.findActiveUsersById(questDialog.authorId!!)!!

            if (userActualizedInfo.tui == questionAuthor.tui){

                val answerCallbackQuery = AnswerCallbackQuery().also {
                    it.callbackQueryId = update.callbackQuery.id
                    it.text = "Вы не можете отвечать самому себе!"
                    it.showAlert = true
                }
                bot.execute(answerCallbackQuery)

                return userActualizedInfo
            }

            if (userActualizedInfo.activeQuestDialog != null) return userActualizedInfo
            val firstMessage = questMessageRepository.findById(params.questDialog.dialogHistory.first()).get()

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text = "Кажется, ты хотел(-а) возобновить диалог по поводу сообщения:",
                ),
            )
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    fromChatId = questionAuthor.tui.toString(),
                    messageId = firstMessage.messageId!!,
                ),
            )
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text =
                    "Ты можешь начать анонимный диалог с пользователем.",
                    replyMarkup = createRecreateStartKeyboard(questDialog),
                ),
            )

            return userActualizedInfo
        }
    }

    private fun clickRecreateStartDialog(params: Params): UserActualizedInfo{
        params.apply {
            if (questDialog.questionStatus != QuestionStatus.IGNORE &&
                questDialog.questionStatus != QuestionStatus.CLOSED
            ) return userActualizedInfo
            if (userActualizedInfo.activeQuestDialog != null) return userActualizedInfo

            val questSegment = QuestSegment(
                questId = questDialog.id,
                startTime = updatesUtil.getDate(update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                responderId = userActualizedInfo.id
            ).let { questSegmentRepository.save(it) }

            questDialogRepository.save(
                questDialog.copy(
                    questionStatus = QuestionStatus.DIALOG,
                    lastQuestSegmentId = questSegment.id
                )
            )
            val questionAuthor = userRepository.findActiveUsersById(questDialog.authorId!!)!!
            userRepository.save(
                questionAuthor.copy(
                    questDialogId = questDialog.id,
                ),
            )
            switchKeyboardService.switchKeyboard(
                questionAuthor.id!!,
                UserKeyboardType.DIALOG_QUEST,
            )
            switchKeyboardService.switchKeyboard(
                userActualizedInfo.id!!,
                UserKeyboardType.DIALOG_QUEST,
            )
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = questionAuthor.tui!!,
                    text = "<i>С вами общается оператор по поводу обращения #${questDialog.id}</i>",
                    parseMode = ParseMode.HTML,
                ),
            )

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text =
                    "<i>Ты перешел в диалог с пользователем ${MessageSenderUtil.userName(
                        questionAuthor.lastTgNick,
                        questionAuthor.fullName,
                    )}. " +
                            "Несмотря на твою анонимность, оставайся вежливым :)</i>",
                    parseMode = ParseMode.HTML,
                ),
            )

            messageSenderService.editMessage(
                MessageParams(
                    chatId = QUEST_RESPONDENT_CHAT_ID,
                    messageId = questDialog.consoleMessageId!!.toInt(),
                    text =
                    "✏\uFE0F ${MessageSenderUtil.userName(userActualizedInfo.lastTgNick, userActualizedInfo.fullName)} " +
                            "ведет диалог",
                ),
            )

            return params.userActualizedInfo.copy(
                activeQuestDialog = params.questDialog,
            )
        }
    }

    private fun createChooseKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Начать диалог")
                        .also { it.callbackData = QUEST_START_DIALOG.format(questDialog.id) },
                ),
            ),
        )

    private fun createRecreateStartKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Начать диалог")
                        .also { it.callbackData = QUEST_RECREATE_START_DIALOG.format(questDialog.id) },
                ),
                listOf(
                    InlineKeyboardButton("\uD83D\uDCAC Посмотреть историю (не паботает)")
                        .also { it.callbackData = QUEST_SHOW_HISTORY.format(questDialog.id) }
                ),
            ),
        )

    private fun createUnbanKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("Разбанить (doesn't work)")
                        // TODO("разбан еще не реализован")
                        .also { it.callbackData = QUEST_BAN.format(questDialog.id) },
                ),
            ),
        )

    private fun createRecreateKeyboard(questDialog: QuestDialog) =
        createKeyboard(
            listOf(
                listOf(
                    InlineKeyboardButton("Переоткрыть диалог")
                        // TODO("разбан еще не реализован")
                        .also { it.callbackData = QUEST_RECREATE.format(questDialog.id) },
                ),
            ),
        )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun getQuestByCallbackData(callbackData: String): QuestDialog? {
        val questId = parseQuestId(callbackData)
        return questDialogRepository.findById(questId).getOrNull()
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

    private data class Params(
        val questDialog: QuestDialog,
        val questSegment: QuestSegment,
        val userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
