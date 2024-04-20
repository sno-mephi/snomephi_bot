package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData

/**
 * Фетчер, обрабатывающий переходы при помощи lastUserActionType
 */
@Component
class CategoryActionTypeHandlerFetcher(
    private val messageSenderService: MessageSenderService,
    private val updatesUtil: UpdatesUtil,
    private val categoryRepository: CategoryRepository,
) : DefaultFetcher() {
    private data class Params(
        val chatId: String,
        val update: Update,
        var userActualizedInfo: UserActualizedInfo,
    )

    @InjectData
    @FetcherPerms(UserRole.CATEGORY_BUILDER)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val params =
            Params(
                chatId,
                update,
                userActualizedInfo,
            )
        when (userActualizedInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_INPUT_START ->
                actionAddTitle(params)

            LastUserActionType.CATEGORY_INPUT_TITLE ->
                actionAddSuffix(params)

            LastUserActionType.CATEGORY_INPUT_SUFFIX ->
                actionAddDescription(params)

            else ->
                return userActualizedInfo
        }
        return params.userActualizedInfo
    }

    private fun actionAddTitle(params: Params) {
        if (params.update.message == null || !params.update.message.hasText()) return
        val messageText = params.update.message.text
        if (TextCommands.isTextCommand(messageText)) return
        if (messageText.length > 64) {
            sendMessage(
                params,
                "❗Слишком длинное сообщение",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        val category = categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) ?: return
        categoryRepository.save(
            Category(
                id = category.id,
                title = messageText,
                suffix = category.suffix,
                description = category.description,
                isUnremovable = category.isUnremovable,
                changedByTui = category.changedByTui,
            ),
        )

        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = params.update.message.messageId,
            ),
        )
        params.userActualizedInfo.data?.categoryMessageId?.let {
            editMessage(
                it,
                params,
                "✏️ Введите тэг категории (до 64 символов):",
                CategoryKeyboards.inputCancel(),
            )
        }
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_INPUT_TITLE,
            )
    }

    private fun actionAddSuffix(params: Params) {
        if (params.update.message == null || !params.update.message.hasText()) return
        if (TextCommands.isTextCommand(params.update.message.text)) return
        val messageText = params.update.message.text.lowercase().replace(' ', '_')
        val category = categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) ?: return
        if (messageText.length > 64) {
            sendMessage(
                params,
                "❗Слишком длинное сообщение",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        if (categoryRepository.findBySuffix(messageText) != null) {
            sendMessage(
                params,
                "❗Категория с таким тэгом уже есть, попробуйте ввести другой",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        if (!messageText.matches(Regex("^[a-z0-9_]+$"))) {
            sendMessage(
                params,
                "❗Тэг может содержать в себе только буквы латинского алфавита или цифры, попробуйте ввести другой",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        categoryRepository.save(
            Category(
                id = category.id,
                title = category.title,
                suffix = messageText,
                description = category.description,
                isUnremovable = category.isUnremovable,
                changedByTui = category.changedByTui,
            ),
        )
        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = params.update.message.messageId,
            ),
        )
        params.userActualizedInfo.data?.categoryMessageId?.let {
            editMessage(
                it,
                params,
                "✏️ Введите описание категории (до 140 символов):",
                CategoryKeyboards.inputCancel(),
            )
        }
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_INPUT_SUFFIX,
            )
    }

    private fun actionAddDescription(params: Params) {
        if (params.update.message == null || !params.update.message.hasText()) return
        val messageText = params.update.message.text
        if (TextCommands.isTextCommand(messageText)) return
        val category = categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) ?: return
        if (messageText.length > 140) {
            sendMessage(
                params,
                "❗Слишком длинное сообщение",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        categoryRepository.save(
            Category(
                id = category.id,
                title = category.title,
                suffix = category.suffix,
                description = messageText,
                isUnremovable = category.isUnremovable,
                changedByTui = category.changedByTui,
            ),
        )
        messageSenderService.deleteMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = params.update.message.messageId,
            ),
        )

        params.userActualizedInfo.data?.categoryMessageId?.let {
            editMessage(
                it,
                params,
                "✏️ Пользователь может отписаться от рассылки?",
                CategoryKeyboards.questionIsUnremovable(),
            )
        }
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_INPUT_DESCRIPTION,
            )
    }

    private fun editMessage(
        messageId: Int,
        params: Params,
        text: String,
        keyboard: InlineKeyboardMarkup?,
    ) {
        val msgId = messageId
        messageSenderService.editMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = msgId,
                text = text,
                replyMarkup = keyboard,
            ),
        )
    }

    private fun sendMessage(
        params: Params,
        text: String,
        keyboard: InlineKeyboardMarkup,
    ) {
        val sendMessage =
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = text,
                    replyMarkup = keyboard,
                ),
            )
        params.userActualizedInfo.data?.categoryMessageId = sendMessage.messageId
    }
}
