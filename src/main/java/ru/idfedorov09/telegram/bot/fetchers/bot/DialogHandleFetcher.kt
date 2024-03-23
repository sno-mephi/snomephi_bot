package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.idfedorov09.telegram.bot.util.MessageSenderUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Фетчер отвечающий за общение челиков в режиме активного диалога
 */
@Component
class DialogHandleFetcher(
    private val messageSenderService: MessageSenderService,
    private val questRepository: QuestRepository,
    private val questDialogMessageRepository: QuestDialogMessageRepository,
    private val userRepository: UserRepository,
    private val switchKeyboardService: SwitchKeyboardService,
) : DefaultFetcher() {
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

        val messageText = update.message.text ?: update.message.caption
        val quest = userActualizedInfo.activeQuest
        val author = userRepository.findActiveUsersById(quest.authorId!!)!!
        val responder = userRepository.findActiveUsersById(quest.responderId!!)!!
        val isByQuestionAuthor = author.tui == userActualizedInfo.tui
        val messageTime =  LocalDateTime.now(
            ZoneId.of("Europe/Moscow"),
        )

        val photoHash =
            if (update.message.hasPhoto()) {
                update.message.photo.last().fileId
            } else {
                null
            }

        val documentHash =
            if (update.message.hasDocument()) {
                update.message.document.fileId
            } else {
                null
            }

        val audioHash =
            if (update.message.hasAudio()) {
                update.message.audio.fileId
            } else {
                null
            }

        val videoHash =
            if (update.message.hasVideo()) {
                update.message.video.fileId
            } else {
                null
            }

        val stickerHash =
            if (update.message.hasSticker()) {
                update.message.sticker.fileId
            } else {
                null
            }

        val voiceHash =
            if (update.message.hasVoice()) {
                update.message.voice.fileId
            } else {
                null
            }

        val videoNoteHash =
            if (update.message.hasVideoNote()) {
                update.message.videoNote.fileId
            } else {
                null
            }

        val params =
            Params(
                messageText = messageText,
                quest = quest,
                author = author,
                responder = responder,
                isByQuestionAuthor = isByQuestionAuthor,
                userActualizedInfo = userActualizedInfo,
                update = update,
                photoHash = photoHash,
                documentHash = documentHash,
                stickerHash = stickerHash,
                voiceHash = voiceHash,
                videoNoteHash = videoNoteHash,
                videoHash = videoHash,
                audioHash = audioHash,
                messageTime = messageTime
            )

        val updatedUserActualizedInfo =
            when {
                TextCommands.isTextCommand(params.messageText) -> handleCommands(params)
                update.hasMessage() && update.message.hasText() -> handleMessageText(params)
                update.hasMessage() && update.message.hasPhoto() -> handleMessagePhoto(params)
                update.hasMessage() && update.message.hasDocument() -> handleMessageDocument(params)
                update.hasMessage() && update.message.hasSticker() -> handleMessageStiker(params)
                update.hasMessage() && update.message.hasVoice() -> handleMessageVoice(params)
                update.hasMessage() && update.message.hasAudio() -> handleMessageAudio(params)
                update.hasMessage() && update.message.hasVideo() -> handleMessageVideo(params)
                update.hasMessage() && update.message.hasVideoNote() -> handleMessageVideoNote(params)
                else -> nonSupportedUpdateType(params)
            }
        return updatedUserActualizedInfo
    }

    private fun nonSupportedUpdateType(params: Params): UserActualizedInfo {
        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "⛔\uFE0F Сообщение данного типа не поддерживается.",
            ),
        )
        return params.userActualizedInfo
    }

    private fun handleMessageAudio(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    messageText = messageText,
                    audioHash = audioHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    text = messageText,
                    audio = InputFile(audioHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageVideo(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    messageText = messageText,
                    videoHash = videoHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    text = messageText,
                    video = InputFile(videoHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageVideoNote(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    videoNoteHash = videoNoteHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    video = InputFile(videoNoteHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageVoice(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    voiceHash = voiceHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    voice = InputFile(voiceHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageStiker(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    stickerHash = stickerHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    sticker = InputFile(stickerHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageDocument(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    messageText = messageText,
                    messageDocumentHash = documentHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                    ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    text = messageText,
                    document = InputFile(documentHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessagePhoto(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    messageText = messageText,
                    messagePhotoHash = photoHash,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    text = messageText,
                    photo = InputFile(photoHash),
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleMessageText(params: Params): UserActualizedInfo {
        params.apply {
            val questDialogMessage =
                QuestDialogMessage(
                    questId = quest.id,
                    isByQuestionAuthor = isByQuestionAuthor,
                    authorId = userActualizedInfo.id,
                    messageText = messageText,
                    messageId = update.message.messageId,
                    messageTime = messageTime,
                ).let { questDialogMessageRepository.save(it) }
            quest.dialogHistory.add(questDialogMessage.id!!)
            questRepository.save(quest)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!,
                    text = messageText!!,
                ),
            )
            return userActualizedInfo
        }
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
                finishTime = params.messageTime
            ),
        )

        userRepository.save(
            params.responder.copy(
                lastUserActionType = LastUserActionType.ACT_QUEST_DIALOG_CLOSE,
            ),
        )

        switchKeyboardService.switchKeyboard(params.author.id!!, UserKeyboardType.DEFAULT_MAIN_BOT)
        switchKeyboardService.switchKeyboard(params.responder.id!!, UserKeyboardType.DEFAULT_MAIN_BOT)

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.author.tui!!,
                text = "_⚠\uFE0F Оператор завершил диалог\\._",
                parseMode = ParseMode.MARKDOWNV2,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.responder.tui!!,
                text = "\uD83D\uDDA4 Спасибо за обратную связь\\! *Диалог завершен\\.*",
                parseMode = ParseMode.MARKDOWNV2,
            ),
        )

        messageSenderService.editMessage(
            MessageParams(
                chatId = GlobalConstants.QUEST_RESPONDENT_CHAT_ID,
                messageId = params.quest.consoleMessageId!!.toInt(),
                text =
                    "✅ ${MessageSenderUtil.userName(params.responder.lastTgNick, params.responder.fullName)} " +
                        "пообщался(-ась)",
            ),
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
        val photoHash: String?,
        val documentHash: String?,
        val stickerHash: String?,
        val voiceHash: String?,
        val videoNoteHash: String?,
        val audioHash: String?,
        val videoHash: String?,
        val quest: Quest,
        val author: User,
        val responder: User,
        val isByQuestionAuthor: Boolean,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        val messageTime: LocalDateTime,
    )
}
