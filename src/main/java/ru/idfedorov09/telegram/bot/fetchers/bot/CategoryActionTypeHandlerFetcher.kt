package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий переходы при помощи lastUserActionType
 */
@Component
class CategoryActionTypeHandlerFetcher(
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {
    private data class RequestData(
        val chatId: String,
        val update: Update,
        var userInfo: UserActualizedInfo,
    )

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData = RequestData(
            chatId,
            update,
            userActualizedInfo,
        )
        when (userActualizedInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_INPUT_START ->
                actionAddTitle(requestData)

            LastUserActionType.CATEGORY_INPUT_TITLE ->
                actionAddSuffix(requestData)

            LastUserActionType.CATEGORY_INPUT_SUFFIX ->
                actionAddDescription(requestData)

            else ->
                return userActualizedInfo
        }
        return requestData.userInfo
    }

    private fun actionAddTitle(data: RequestData) {
        if (data.update.message == null || !data.update.message.hasText()) return
        val messageText = data.update.message.text
        if (TextCommands.isTextCommand(messageText)) return
        if (messageText.length > 64) {
            sendMessage(
                data,
                "❗Слишком длинное сообщение",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        val category = categoryRepository.findByChangedByTui(data.userInfo.tui) ?: return
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
        bot.execute(
            DeleteMessage(
                data.chatId,
                data.update.message.messageId,
            ),
        )
        data.userInfo.data?.toInt()?.let {
            editMessage(
                it,
                data,
                "✏️ Введите тэг категории (до 64 символов):",
                CategoryKeyboards.inputCancel(),
            )
        }
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_INPUT_TITLE,
        )
    }

    private fun actionAddSuffix(data: RequestData) {
        if (data.update.message == null || !data.update.message.hasText()) return
        if (TextCommands.isTextCommand(data.update.message.text)) return
        val messageText = data.update.message.text.lowercase().replace(' ', '_')
        val category = categoryRepository.findByChangedByTui(data.userInfo.tui) ?: return
        if (messageText.length > 64) {
            sendMessage(
                data,
                "❗Слишком длинное сообщение",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        if (categoryRepository.findBySuffix(messageText) != null) {
            sendMessage(
                data,
                "❗Категория с таким тэгом уже есть, попробуйте ввести другой",
                CategoryKeyboards.inputCancel(),
            )
            return
        }
        if (!messageText.matches(Regex("^[a-z0-9_]+$"))) {
            sendMessage(
                data,
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
        bot.execute(
            DeleteMessage(
                data.chatId,
                data.update.message.messageId,
            ),
        )
        data.userInfo.data?.toInt()?.let {
            editMessage(
                it,
                data,
                "✏️ Введите описание категории (до 1024 символов):",
                CategoryKeyboards.inputCancel(),
            )
        }
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_INPUT_SUFFIX,
        )
    }

    private fun actionAddDescription(data: RequestData) {
        if (data.update.message == null || !data.update.message.hasText()) return
        val messageText = data.update.message.text
        if (TextCommands.isTextCommand(messageText)) return
        val category = categoryRepository.findByChangedByTui(data.userInfo.tui) ?: return
        if (messageText.length > 1024) {
            sendMessage(
                data,
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
        bot.execute(
            DeleteMessage(
                data.chatId,
                data.update.message.messageId,
            ),
        )
        data.userInfo.data?.toInt()?.let {
            editMessage(
                it,
                data,
                "✏️ Пользователь может отписаться от рассылки?",
                CategoryKeyboards.questionIsUnremovable(),
            )
        }
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_INPUT_DESCRIPTION,
        )
    }

    private fun editMessage(messageId: Int, data: RequestData, text: String, keyboard: InlineKeyboardMarkup?) {
        val msgId = messageId
        bot.execute(
            EditMessageText(
                data.chatId,
                msgId,
                null,
                text,
                null,
                null,
                keyboard,
                null,
            ),
        )
    }

    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup) {
        val msg = SendMessage(data.chatId, text)
        msg.replyMarkup = keyboard
        val lastSent = bot.execute(msg).messageId
        data.userInfo = data.userInfo.copy(
            data = lastSent.toString(),
        )
    }
}
