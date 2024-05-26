package ru.idfedorov09.telegram.bot.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.ConfigParams
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.ECallbackData
import ru.idfedorov09.telegram.bot.data.model.CloseDialogMessages
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.ECallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import java.time.LocalDateTime

@Service
class DialogService(
    private val questDialogRepository: QuestDialogRepository,
    private val questSegmentRepository: QuestSegmentRepository,
    private val userRepository: UserRepository,
    private val switchKeyboardService: SwitchKeyboardService,
    private val messageSenderService: MessageSenderService,
    private val ECallbackDataRepository: ECallbackDataRepository,
    private val configParamsService: ConfigParamsService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(DialogService::class.java)
    }

    fun closeDialog(
        questDialog: QuestDialog,
        finishTime: LocalDateTime?,
        isByQuestionAuthor: Boolean,
        author: User,
        responder: User,
        currentUserActualizedInfo: UserActualizedInfo,
        closeDialogMessages: CloseDialogMessages,
        showRecreateButton: Boolean = true,
    ): UserActualizedInfo {
        val questSegment = questDialog.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() } ?: run {
            log.error("Can't find questSegment for questDialog=$questDialog")
            return currentUserActualizedInfo
        }

        questDialogRepository.save(
            questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = finishTime,
            ),
        )

        questSegmentRepository.save(
            questSegment.copy(
                finishTime = finishTime,
            ),
        )

        userRepository.save(
            responder.copy(
                lastUserActionType = LastUserActionType.ACT_QUEST_DIALOG_CLOSE,
            ),
        )

        switchKeyboardService.switchKeyboard(author.id!!, UserKeyboardType.DEFAULT_MAIN_BOT)
        switchKeyboardService.switchKeyboard(responder.id!!, UserKeyboardType.DEFAULT_MAIN_BOT)

        if (isByQuestionAuthor) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = responder.tui!!,
                    text = closeDialogMessages.onAuthorClose.toResponder,
                    parseMode = ParseMode.HTML,
                ),
            )

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = author.tui!!,
                    text = closeDialogMessages.onAuthorClose.toAuthor,
                    parseMode = ParseMode.HTML,
                ),
            )

            userRepository.save(
                responder.copy(
                    lastUserActionType = null,
                    questDialogId = null,
                ),
            )
        } else {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = author.tui!!,
                    text = closeDialogMessages.onResponderClose.toAuthor,
                    parseMode = ParseMode.HTML,
                ),
            )

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = responder.tui!!,
                    text = closeDialogMessages.onResponderClose.toResponder,
                    parseMode = ParseMode.HTML,
                ),
            )
        }

        val recreateDialog =
            ECallbackData(
                callbackData = CallbackCommands.QUEST_RECREATE.format(questDialog.id),
                metaText = "\uD83D\uDD01 Переоткрыть диалог",
            ).save()

        messageSenderService.editMessage(
            MessageParams(
                chatId = configParamsService.getValue(ConfigParams.ADMIN_CHAT_ID)!!,
                messageId = questDialog.consoleMessageId!!.toInt(),
                text = closeDialogMessages.consoleResultText
                    ?: "✅ ${MessageSenderUtil.userName(responder.lastTgNick, responder.fullName)} пообщался(-ась)",
                replyMarkup = if (showRecreateButton) createKeyboard(recreateDialog) else createKeyboard()
            ),
        )

        return currentUserActualizedInfo.copy(
            lastUserActionType = null,
            activeQuestDialog = null,
        )
    }

    private fun createKeyboard(vararg ECallbackData: ECallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*ECallbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun ECallbackData.save() = ECallbackDataRepository.save(this)
}