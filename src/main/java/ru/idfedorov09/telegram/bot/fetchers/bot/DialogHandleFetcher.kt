package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер отвечающий за общение челиков в режиме активного диалога
 */
@Component
class DialogHandleFetcher(
    private val messageSenderService: MessageSenderService,
    private val questRepository: QuestRepository,
    private val questDialogMessageRepository: QuestDialogMessageRepository,
    private val userRepository: UserRepository,
) : GeneralFetcher() {

    // TODO: добавить поддержку картинок, файлов, HTML/MARKDOWN-разметки
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        // если пользователь не в активном диалоге то скипаем фетчер
        if (userActualizedInfo.activeQuest == null) return userActualizedInfo

        if (!update.hasMessage()) return userActualizedInfo

        // если апдейт из беседы, то игнорим
        if (update.message.chatId.toString() != userActualizedInfo.tui) return userActualizedInfo

        val messageText = update.message.text
        val quest = userActualizedInfo.activeQuest
        val author = userRepository.findById(quest.authorId!!).get()
        val responder = userRepository.findById(quest.responderId!!).get()
        val isByQuestionAuthor = author.tui == userActualizedInfo.tui

        val params = Params(
            messageText = messageText,
            quest = quest,
            author = author,
            responder = responder,
            isByQuestionAuthor = isByQuestionAuthor,
            userActualizedInfo = userActualizedInfo,
            update = update,
        )

        val updatedUserActualizedInfo = when {
            TextCommands.isTextCommand(params.messageText) -> handleCommands(params)
            update.hasMessage() && update.message.hasText() -> handleMessageText(params)
            update.hasMessage() && update.message.hasPhoto() -> handleMessagePhoto(params)
            else -> nonSupportedUpdateType(params)
        }
        return updatedUserActualizedInfo
    }

    private fun nonSupportedUpdateType(params: Params): UserActualizedInfo {
        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "⛔\uFE0F Сообщение данного типа не поддерживается."
            )
        )
        return params.userActualizedInfo
    }

    private fun handleMessagePhoto(params: Params): UserActualizedInfo {
        nonSupportedUpdateType(params)
        // TODO()
        return params.userActualizedInfo
    }

    private fun handleMessageText(params: Params): UserActualizedInfo {
        val quest = params.quest
        val isByQuestionAuthor = params.isByQuestionAuthor
        val userActualizedInfo = params.userActualizedInfo
        val messageText = params.messageText
        val update = params.update
        val responder = params.responder
        val author = params.author

        val questDialogMessage = QuestDialogMessage(
            questId = quest.id,
            isByQuestionAuthor = isByQuestionAuthor,
            authorId = userActualizedInfo.id,
            messageText = messageText,
            messageId = update.message.messageId,
        ).let { questDialogMessageRepository.save(it) }
        quest.dialogHistory.add(questDialogMessage.id!!)
        questRepository.save(quest)

        messageSenderService.sendMessage(
            MessageParams(
                chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                text = messageText!!
            )
        )

        return userActualizedInfo
    }

    private fun handleCommands(params: Params): UserActualizedInfo {
        return when (params.messageText) {
            TextCommands.QUEST_DIALOG_CLOSE.commandText -> closeDialog(params)
            else -> params.userActualizedInfo
        }
    }

    private fun closeDialog(params: Params): UserActualizedInfo {
        questRepository.save(
            params.quest.copy(
                questionStatus = QuestionStatus.CLOSED,
            ),
        )

        userRepository.save(
            params.responder.copy(
                lastUserActionType = LastUserActionType.ACT_QUEST_DIALOG_CLOSE,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.author.tui!!,
                text = "_⚠\uFE0F Оператор завершил диалог\\._",
                parseMode = ParseMode.MARKDOWNV2
            )
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.responder.tui!!,
                text = "\uD83D\uDDA4 Спасибо за обратную связь\\! *Диалог завершен\\.*",
                replyMarkup = ReplyKeyboardRemove().apply {
                    removeKeyboard = true
                },
                parseMode = ParseMode.MARKDOWNV2
            )
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = GlobalConstants.QUEST_RESPONDENT_CHAT_ID,
                messageId = params.quest.consoleMessageId!!.toInt(),
                newText = "✅ @${params.responder.lastTgNick} пообщался(-ась)"
            )
        )

        return params.userActualizedInfo.copy(
            lastUserActionType = null,
        )
    }

    /**
     * Вспомогательный класс для передачи параметров
     */
    private data class Params(
        val messageText: String?,
        val quest: Quest,
        val author: User,
        val responder: User,
        val isByQuestionAuthor: Boolean,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
    )
}
