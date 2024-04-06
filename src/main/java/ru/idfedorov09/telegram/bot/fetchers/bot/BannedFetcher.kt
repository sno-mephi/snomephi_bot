package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.GlobalConstants.BOT_TIME_ZONE
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BanRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.security.cert.CertPathValidatorException.Reason
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@Component
class BannedFetcher(
    private val updatesUtil: UpdatesUtil,
    private val messageSenderService: MessageSenderService,
    private val callbackDataRepository: CallbackDataRepository,
    private val userRepository: UserRepository,
    private val banRepository: BanRepository,
) : DefaultFetcher() {

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
    @InjectData
    @FetcherPerms(UserRole.MODERATOR)
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    )  {
        val params = Params(userActualizedInfo, update, updatesUtil)
        when{
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> return
        }
    }


    private fun textCommandsHandler(params: Params)  {
        val text = params.update.message.text
        text.apply {
            when {
                startsWith(TextCommands.BANNED_COMMAND.invoke()) -> entryUserTui(params, isBan = true)
                startsWith(TextCommands.UNBANNED_COMMAND.invoke()) -> entryUserTui(params, isBan = false)
                else -> commonTextHandler(params)
            }
        }
    }
    private fun commonTextHandler(params: Params)  {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.BANED_ENTER_TUI -> handleTui(params, isBan = true)
            LastUserActionType.UNBANED_ENTER_TUI -> handleTui(params, isBan = false)
            LastUserActionType.BANNED_ENTER_REASON -> reasonBan(params)
            LastUserActionType.BANNED_ENTER_FINISH_TIME -> dateFinishBan(params, permanent = false)
            else -> return
        }
    }

    private fun entryUserTui(params: Params, isBan: Boolean)  {
        params.userActualizedInfo.apply {
            val text = "Следующим сообщением напиши мне Telegram User Id человека, которого ты хочешь забанить/разбанить"
            val cancel =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CANCEL.data,
                    metaText = "отмена",
                ).save()
            val sentMessage = messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = text, replyMarkup = createKeyboard(cancel),
                ),
            )
            if (isBan) {
                banData =
                    banRepository.save(
                        Ban(
                            moderatorId = id,
                            lastConsoleMessageId = sentMessage.messageId
                        ),
                    )
                lastUserActionType = LastUserActionType.BANED_ENTER_TUI

            } else {
                lastUserActionType = LastUserActionType.UNBANED_ENTER_TUI
            }
        }
    }

    private fun handleTui(params: Params, isBan: Boolean) {
        params.apply {
            val cancel =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CANCEL.data,
                    metaText = "отмена",
                ).save()
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = update.message.messageId
                )
            )
            val tui =
                update.message.text.toLongOrNull() ?: run {
                    messageSenderService.editMessage(
                        MessageParams(
                            chatId = userActualizedInfo.tui,
                            text = "Некорректный tui. Повтори попытку",
                            messageId = userActualizedInfo.banData?.lastConsoleMessageId,
                            replyMarkup = createKeyboard(cancel)
                        ),
                    )
                    return
                }
            if (tui.toString() == userActualizedInfo.tui){
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = "Невозможно заблокировать себя",
                        messageId = userActualizedInfo.banData?.lastConsoleMessageId,
                        replyMarkup = createKeyboard(cancel)
                    ),
                )
                return
            }
            val user =
                userRepository.findByTui(tui.toString()) ?: run {
                    messageSenderService.editMessage(
                        MessageParams(
                            chatId = userActualizedInfo.tui,
                            text = "Такого юзера нет, повтори попытку",
                            messageId = userActualizedInfo.banData?.lastConsoleMessageId,
                            replyMarkup = createKeyboard(cancel)
                        ),
                    )
                    return
                }
            if (isBan) {
                userActualizedInfo.banData =
                    userActualizedInfo.banData?.copy(
                        userTui = tui.toString(),

                        )
                val text = "Нашел пользователя ${user.fullName}.\nПожалуйста, укажите причину бана текстом."
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = text,
                        messageId = userActualizedInfo.banData?.lastConsoleMessageId,
                        replyMarkup = createKeyboard(cancel)
                    )
                )
                userActualizedInfo.lastUserActionType = LastUserActionType.BANNED_ENTER_REASON
            } else {
                val confirm =
                    CallbackData(
                        callbackData = CallbackCommands.UNBANNED_CONFIRM.data + "|${tui}",
                        metaText = "подтвердите разблокировку"
                    ).save()
                val text = "Нашел пользователя ${user.fullName}.\nПожалуйста, подтвердите его разблокировку."
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        text = text,
                        replyMarkup = createKeyboard(cancel, confirm)
                    )
                )
                userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
            }
        }
    }

    private fun reasonBan(
        params: Params,
        prefix: String? = null,
    ) {
        params.apply {
            val cancel =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CANCEL.data,
                    metaText = "отмена",
                ).save()
            val permaBan =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_PERMANENT.data,
                    metaText = "забанить навсегда"
                ).save()
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = update.message.messageId
                )
            )
            userActualizedInfo.banData =
                userActualizedInfo.banData?.copy(
                    text = update.message.text
                )
            userActualizedInfo.lastUserActionType = LastUserActionType.BANNED_ENTER_FINISH_TIME
            val textStart = prefix?.let { "$prefix\n" } ?: ""
            val text = textStart + "Пожалуйста, укажите время завершения бана в формате дд.мм.гггг чч:мм"

            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = text,
                    messageId = userActualizedInfo.banData?.lastConsoleMessageId,
                    replyMarkup = createKeyboard(cancel, permaBan)
                )
            )
        }
    }

    private fun dateFinishBan(params: Params, permanent: Boolean)  {
        params.userActualizedInfo.apply {
            val finishTime = if (permanent) {
                resolveFullDate("31.12.9999 23:59")
            }
            else {
                 when {
                    params.update.message.text.matches(Regex("\\d{2}.\\d{2}.\\d{4} \\d{2}:\\d{2}")) ->
                        resolveFullDate(params.update.message.text)
                    else -> null
                } ?: run {
                    reasonBan(params, prefix = "Неверный формат времени")
                    return
                }
            }
            banData =
                banData?.copy(
                    finishTime = finishTime,
                )
            val cancel =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CANCEL.data,
                    metaText = "отмена",
                ).save()
            val confirm =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CONFIRM.data,
                    metaText = "подтвердите бан"
                ).save()

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = "Вы уверены, что хотите заблокировать пользователя ${banData?.userTui} " +
                            " по причине \n${banData?.text}",
                    messageId = banData?.lastConsoleMessageId,
                    replyMarkup = createKeyboard(cancel, confirm)
                )
            )

            lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun resolveFullDate(fullDateText: String) = LocalDateTime.parse(fullDateText, FORMATTER)
        .atZone(BOT_TIME_ZONE).toLocalDateTime()

    private fun confirmBan(params: Params) {
        params.userActualizedInfo.apply{
            banData =
                banData?.copy(
                    isBuilt = true,
                    startTime = params.updatesUtil.getDate(params.update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
                )
            val unBan =
                CallbackData(
                    callbackData = CallbackCommands.UNBANNED_USER.data + "|${banData?.userTui}",
                    metaText = "разблокировать",
                ).save()
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = "Вы успешно заблокировали пользователя ${banData?.userTui} до " +
                            "${banData?.finishTime?.format(FORMATTER)} по причине \n${banData?.text}",
                    messageId = banData?.lastConsoleMessageId,
                    replyMarkup = createKeyboard(unBan)
                )
            )
        }
    }

    private fun confirmUnban(
        params: Params,
        callbackData: String
    ) {
        params.apply {
            val tui = callbackData.split("|").last()
            banRepository.unbanUser(tui)
            messageSenderService.editMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = "Вы успешно заблокировали пользователя ${tui}.",
                    messageId = userActualizedInfo.banData?.lastConsoleMessageId
                )
            )
        }
    }

    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.BANNED_USER.data) -> clickBackBan(params, this)
                startsWith(CallbackCommands.UNBANNED_USER.data) -> clickUnban(params, this)
                startsWith(CallbackCommands.BANNED_CANCEL.data) -> banCancel(params)
                startsWith(CallbackCommands.BANNED_PERMANENT.data) -> dateFinishBan(params, permanent = true)
                startsWith(CallbackCommands.BANNED_CONFIRM.data) -> confirmBan(params)
                startsWith(CallbackCommands.UNBANNED_CONFIRM.data) -> confirmUnban(params, this)
            }
        }
    }

    private fun clickBackBan(
        params: Params,
        callbackData: String
    ) {
        params.apply {
            val tui = callbackData.split("|").last()
            val user = userRepository.findByTui(tui) ?: return
            val cancel =
                CallbackData(
                    callbackData = CallbackCommands.BANNED_CANCEL.data,
                    metaText = "отмена",
                ).save()

            if (userActualizedInfo.tui == tui) {
                messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = "Вы не можете заблокировать себя",
                    )
                )
                banCancel(params)
                return
            }

            val text = "Нашел пользователя ${user.fullName}.\nПожалуйста, укажите причину бана текстом."
            userActualizedInfo.lastUserActionType = LastUserActionType.BANNED_ENTER_REASON

            val sentMessage = messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = text,
                    replyMarkup = createKeyboard(cancel)
                )
            )

            userActualizedInfo.banData =
                userActualizedInfo.banData?.copy(
                    userTui = tui,
                    lastConsoleMessageId = sentMessage.messageId
                )
        }
    }

    private fun clickUnban(
        params: Params,
        callbackData: String,
    ) {
        val tui = callbackData.split("|").last()
        val user = userRepository.findByTui(tui) ?: return

    }

    private fun banCancel(params: Params) {
        removeBanConsole(params)
        params.userActualizedInfo.banData?.let {
            banRepository.save(
                it.copy(
                    isDeleted = true,
                ),
            )
        }
        params.userActualizedInfo.banData = null
    }

    private fun removeBanConsole(params: Params) {
        params.userActualizedInfo.apply {
            banData ?: return
            banData?.lastConsoleMessageId ?: return

            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = banData?.lastConsoleMessageId!!,
                ),
            )
            banData =
                banData?.copy(
                    lastConsoleMessageId = null,
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

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
        val updatesUtil: UpdatesUtil,
    )
}
