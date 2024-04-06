package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class BannedFetcher(
    private val messageSenderService: MessageSenderService,
    private val  callbackData: CallbackData,
    private val callbackDataRepository: CallbackDataRepository,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    ) {
        val params = Params(userActualizedInfo, update)
        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> return
        }
    }


    private fun textCommandsHandler(params: Params) {
        val text = params.update.message.text
        text.apply {
            when {
                startsWith(TextCommands.BANNED_COMMAND.invoke()) -> entryUserTui(params)
                else -> commonTextHandler(params)
            }
        }
    }
    private fun commonTextHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.BANED_ENTER_TUI -> handleTui(params)
            else -> return
        }
    }

    private fun entryUserTui(params: Params){
        val text = "Следующим сообщением напиши мне Telegram User Id человека, которого хочешь забанить/разбанить"
        val cancel =
            callbackDataRepository.save(
                CallbackData(
                    callbackData = "#perms_cancel",
                    metaText = "отмена",
                )
            )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = text,
                replyMarkup = createKeyboard(cancel),
            ),
        )

        params.userActualizedInfo.copy(
            lastUserActionType = LastUserActionType.PERMS_ENTER_TUI,
        )
    }

    private fun handleTui(params: Params) {

    }
    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}
