package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.jvm.optionals.getOrNull

/**
 фетчер для реализации команды  /reset (мягкое удаление пользователя)
 */
@Component
class DeleteUserFetcher(
    private val userRepository: UserRepository,
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
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
            update.hasMessage()
                    && update.message.hasText()
                    && update.message.text.startsWith(TextCommands.RESET()) -> textCommandResetHandler(params)
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
                text =  "Вы действительно хотите удалить аккаунт?",
                replyMarkup = createKeyboard(keyboard)
            ),
        )
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
        return params.userActualizedInfo
    }

    private fun deleteAccount(params: Params): UserActualizedInfo {


        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                messageId = params.update.callbackQuery.message.messageId
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text =  "Аккаунт удалён",
            ),
        )

        params.userActualizedInfo.isDeleted = true

        return params.userActualizedInfo
    }

    private fun noDeleteAccount(params: Params): UserActualizedInfo  {

        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                messageId = params.update.callbackQuery.message.messageId
            ),
        )

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text =  "Хорошо, продолжаем работу",
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
}
