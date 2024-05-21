package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.executor.Executor
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.enums.*
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_ANSWER
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_IGNORE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_RECREATE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_RECREATE_START_DIALOG
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_SHOW_HISTORY
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands.QUEST_START_DIALOG
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.QuestSegment
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.*
import ru.idfedorov09.telegram.bot.service.ConfigParamsService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.lang.NumberFormatException
import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull

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
    private val callbackDataRepository: CallbackDataRepository,
    private val configParamsService: ConfigParamsService,
) : DefaultFetcher() {
    // TODO: обработать случай когда бот не может написать пользователю!
    // TODO: нельзя отвечать самому себе
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (!update.hasCallbackQuery()) return userActualizedInfo

        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: userActualizedInfo
        val callbackData = callbackId?.let { callbackDataRepository.findById(it).getOrNull() } ?: return userActualizedInfo

        if (!callbackData.callbackData?.let { Regex("^.*\\|\\d+$").matches(it) }!! ||
            !userActualizedInfo.isRegistered
        ) {
            return userActualizedInfo
        }

        val questByCallbackData = getQuestByCallbackData(callbackData.callbackData) ?: return userActualizedInfo
        val segment = questByCallbackData.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() }

        val params =
            Params(
                questByCallbackData,
                segment!!,
                userActualizedInfo,
                update,
            )
        return when {
            QUEST_ANSWER.isMatch(callbackData.callbackData) -> clickAnswer(params)
            QUEST_IGNORE.isMatch(callbackData.callbackData) -> clickIgnore(params)
            QUEST_START_DIALOG.isMatch(callbackData.callbackData) -> clickStartDialog(params)
            QUEST_RECREATE.isMatch(callbackData.callbackData) -> clickRecreate(params)
            QUEST_RECREATE_START_DIALOG.isMatch(callbackData.callbackData) -> clickRecreateStartDialog(params)
            else -> userActualizedInfo
        }
    }

    private fun clickStartDialog(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus != QuestionStatus.WAIT) {
            val callbackAnswer =
                AnswerCallbackQuery().also {
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
            ),
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
                chatId = configParamsService.getValue(ConfigParams.ADMIN_CHAT_ID)!!,
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
                finishTime =
                updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
            ),
        )

        questSegmentRepository.save(
            params.questSegment.copy(
                responderId = params.userActualizedInfo.id,
                finishTime =
                updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
            ),
        )

        val newText = "\uD83D\uDFE1 Проигнорировано пользователем ${MessageSenderUtil.userName(
            params.userActualizedInfo.lastTgNick,
            params.userActualizedInfo.fullName,
        )}."
        val recreateDialog =
            CallbackData(
                callbackData = QUEST_RECREATE.format(params.questDialog.id),
                metaText = "Переоткрыть диалог",
            ).save()
        messageSenderService.editMessage(
            MessageParams(
                chatId = configParamsService.getValue(ConfigParams.ADMIN_CHAT_ID)!!,
                messageId = params.questDialog.consoleMessageId?.toInt(),
                text = newText,
                replyMarkup = createKeyboard(recreateDialog)
            ),
        )
        return params.userActualizedInfo
    }

    private fun clickAnswer(params: Params): UserActualizedInfo {
        if (params.questDialog.questionStatus != QuestionStatus.WAIT) return params.userActualizedInfo

        val questionAuthor = userRepository.findActiveUsersById(params.questDialog.authorId!!)!!

        if (params.userActualizedInfo.tui == questionAuthor.tui) {
            val answerCallbackQuery =
                AnswerCallbackQuery().also {
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

        val startDialog = CallbackData(
            callbackData = QUEST_START_DIALOG.format(params.questDialog.id),
            metaText = "\uD83D\uDCAC Начать диалог"
        ).save()

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Ты можешь начать анонимный диалог с пользователем:",
                replyMarkup = createKeyboard(startDialog),
            ),
        )

        return params.userActualizedInfo
    }

    private fun clickRecreate(params: Params): UserActualizedInfo {
        params.apply {
            if (questDialog.questionStatus != QuestionStatus.IGNORE &&
                questDialog.questionStatus != QuestionStatus.CLOSED
            ) {
                return userActualizedInfo
            }

            val questionAuthor = userRepository.findActiveUsersById(questDialog.authorId!!)!!

            if (userActualizedInfo.tui == questionAuthor.tui) {
                val answerCallbackQuery =
                    AnswerCallbackQuery().also {
                        it.callbackQueryId = update.callbackQuery.id
                        it.text = "Вы не можете переоткрывать свое обращение!"
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
            val recreateStartDialog =
                CallbackData(
                    callbackData = QUEST_RECREATE_START_DIALOG.format(questDialog.id),
                    metaText = "Начать диалог",
                ).save()
            val showHistory =
                CallbackData(
                    callbackData = QUEST_SHOW_HISTORY.format(questDialog.id),
                    metaText = "\uD83D\uDCAC Посмотреть историю (не паботает)"
                ).save()
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.userActualizedInfo.tui,
                    text =
                    "Ты можешь начать анонимный диалог с пользователем.",
                    replyMarkup = createKeyboard(recreateStartDialog, showHistory)
                ),
            )

            return userActualizedInfo
        }
    }

    private fun clickRecreateStartDialog(params: Params): UserActualizedInfo {
        params.apply {
            if (questDialog.questionStatus != QuestionStatus.IGNORE &&
                questDialog.questionStatus != QuestionStatus.CLOSED
            ) {
                return userActualizedInfo
            }
            if (userActualizedInfo.activeQuestDialog != null) return userActualizedInfo

            val questSegment =
                QuestSegment(
                    questId = questDialog.id,
                    startTime =
                    updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                    responderId = userActualizedInfo.id,
                ).let { questSegmentRepository.save(it) }

            questDialogRepository.save(
                questDialog.copy(
                    questionStatus = QuestionStatus.DIALOG,
                    lastQuestSegmentId = questSegment.id,
                ),
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
                    text = "<i>Оператор возобновил диалог по поводу обращения #${questDialog.id}</i>",
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
                    chatId = configParamsService.getValue(ConfigParams.ADMIN_CHAT_ID)!!,
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

    private fun createKeyboard(vararg callbackData: CallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*callbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

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