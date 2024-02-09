package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.TextCommands.BROADCAST_CONSTRUCTOR
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Конструктор рассылки
 */
@Component
class BroadcastConstructorFetcher(
    private val callbackDataRepository: CallbackDataRepository,
) : GeneralFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!update.hasMessage()) return
        val text = update.message.text

        val params = Params(
            bot,
            userActualizedInfo,
        )

        text.apply {
            when {
                startsWith(BROADCAST_CONSTRUCTOR()) -> showConsole(params)
            }
        }
    }

    private fun showConsole(
        params: Params
    ) {
        if (params.userActualizedInfo.constructorData == null) {
            val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"
            val newPhoto = CallbackData(callbackData = "#bc_new_photo", metaText = "Добавить фото").save()
            val addText = CallbackData(callbackData = "#bc_change_text", metaText = "Добавить текст").save()
            val addButton = CallbackData(callbackData = "#bc_add_button", metaText = "Добавить кнопку").save()
            val cancelButton = CallbackData(callbackData = "#bc_cancel", metaText = "Отмена").save()

            val keyboard =
                listOf(newPhoto, addText, addButton, cancelButton).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.callbackData = button.callbackData
                    }
                }.map { listOf(it) }

            params.bot.execute(
                SendMessage().also {
                    it.text = messageText
                    it.parseMode = ParseMode.HTML
                    it.replyMarkup = createKeyboard(keyboard)
                    it.chatId = params.userActualizedInfo.tui
                },
            )
            return
        } else {
            // TODO: редактируем сообщение
        }
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        val bot: Executor,
        val userActualizedInfo: UserActualizedInfo,
    )
}
