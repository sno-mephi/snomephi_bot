package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.executor.Executor
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.*
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull
import ru.idfedorov09.telegram.bot.repo.QuestSegmentRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
/**
 фетчер для реализации команды  /reset (мягкое удаление пользователя)
 */
@Component
class DeleteUserFetcher(
    private val updatesUtil: UpdatesUtil,
    private val questDialogRepository: QuestDialogRepository,
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
    private val updateDataFetcher: UpdateDataFetcher,
    private val questSegmentRepository: QuestSegmentRepository,
    private val userRepository: UserRepository,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ): UserActualizedInfo {
        val params =
            Params(
                bot,
                userActualizedInfo,
                update,
            )

        return when {
            update.hasCallbackQuery() -> callbackCommandResetHandler(params)
            update.hasMessage() &&
                update.message.hasText() &&
                update.message.text.startsWith(TextCommands.RESET()) -> textCommandResetHandler(params)
            else -> userActualizedInfo
        }
    }

    private fun textCommandResetHandler(params: Params): UserActualizedInfo {
        val confirmDel =
            CallbackData(
                callbackData = "#confirm_delete",
                metaText = "Да, хочу удалить аккаунт",
            ).save()

        val cancelDel =
            CallbackData(
                callbackData = "#cancel_delete",
                metaText = "Отмена",
            ).save()

        val keyboard =
            listOf(confirmDel, cancelDel).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Вы действительно хотите удалить аккаунт? При удалении все ваши обращения останутся без ответа.",
                replyMarkup = createKeyboard(keyboard),
            ),
        )

        stopFlowNextExecution()
        return params.userActualizedInfo
    }

    private fun callbackCommandResetHandler(params: Params): UserActualizedInfo {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return params.userActualizedInfo
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return params.userActualizedInfo

        callbackData.callbackData?.apply {
            return when {
                startsWith("#confirm_delete") -> deleteAccount(params)
                startsWith("#cancel_delete") -> noDeleteAccount(params)
                else -> params.userActualizedInfo
            }
        }

        stopFlowNextExecution()
        return params.userActualizedInfo
    }

    private fun deleteAccount(params: Params): UserActualizedInfo {
        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                messageId = params.update.callbackQuery.message.messageId,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Аккаунт удалён",
            ),
        )

       val messagesToDelete = questDialogRepository.findAllMessageOfDeletedUser(params.userActualizedInfo.id)
        messagesToDelete.forEach { message ->

            messageSenderService.editMessage(
                MessageParams(
                    messageId = message?.toIntOrNull(),
                    chatId = GlobalConstants.QUEST_RESPONDENT_CHAT_ID,
                    text ="Пользователь удалён, ответ невозможен.",
                ),
            )
        }


        if (params.userActualizedInfo.activeQuestDialog != null){
            val quest = params.userActualizedInfo.activeQuestDialog
            val segment = quest?.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() }
            val responder = userRepository.findActiveUsersById(segment?.responderId!!)!!
            val author = userRepository.findActiveUsersById(quest.authorId!!)!!
            val messageTime =
                updatesUtil.getDate(params.update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }

            val userParams = UserParams(
                    questDialog = quest,
                    questSegment = segment,
                    author = author,
                    responder = responder,
                    userActualizedInfo = params.userActualizedInfo,
                    update = params.update,
                    messageTime = messageTime,
                )
            params.userActualizedInfo = closeDialog(userParams,params)
        }


        params.userActualizedInfo.isDeleted = true
        updateDataFetcher.doFetch(
            userActualizedInfo = params.userActualizedInfo,
            update = params.update,
        )
        stopFlowNextExecution()

        return params.userActualizedInfo
    }

    private fun noDeleteAccount(params: Params): UserActualizedInfo {
        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                messageId = params.update.callbackQuery.message.messageId,
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = "Хорошо, продолжаем работу",
            ),
        )

        return params.userActualizedInfo
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        val bot: Executor,
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
    private data class UserParams(
        val questDialog: QuestDialog,
        val questSegment: QuestSegment,
        val author: User,
        val responder: User,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        val messageTime: LocalDateTime?,
    )


    private fun closeDialog(userParams: UserParams,params: Params): UserActualizedInfo {
        questDialogRepository.save(
            userParams.questDialog.copy(
                questionStatus = QuestionStatus.CLOSED,
                finishTime = userParams.messageTime,
            ),
        )

        questSegmentRepository.save(
            userParams.questSegment.copy(
                finishTime = userParams.messageTime,
            ),
        )

        userRepository.save(
            userParams.responder.copy(
                lastUserActionType = LastUserActionType.ACT_QUEST_DIALOG_CLOSE,
            ),
        )

        if (params.userActualizedInfo.tui == userParams.author.tui) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userParams.responder.tui!!,
                    text = "Пользователь удалил профиль, диалог завершен.",
                ),
            )

            userRepository.save(
                userParams.responder.copy(
                    lastUserActionType = null,
                    questDialogId = null,
                ),
            )
        } else {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userParams.author.tui!!,
                    text = "Оператор удалил профиль (технические проблемы), диалог завершён. Попробуйте написать в поддержку ещё раз.",
                ),
            )
        }
        return userParams.userActualizedInfo.copy(
            lastUserActionType = null,
            activeQuestDialog = null,
        )
    }
}
