package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий нажатия на кнопки категорий
 */
@Component
class CategoryButtonHandlerFetcher(
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {
    private data class RequestData(
        val chatId: String,
        val update: Update,
        var userInfo: UserActualizedInfo,
    )

    val pageSize: Long = 6

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (update.callbackQuery == null) return userActualizedInfo
        val callbackData = update.callbackQuery.data
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val requestData = RequestData(
            chatId,
            update,
            userActualizedInfo,
        )
        when {
            CallbackCommands.CATEGORY_ACTION_MENU.isMatch(callbackData) ->
                clickActionMenu(requestData, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CHOOSE_MENU.isMatch(callbackData) ->
                clickChooseMenu(requestData, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_EDIT.isMatch(callbackData) ->
                clickEdit(requestData)

            CallbackCommands.CATEGORY_ADD.isMatch(callbackData) ->
                clickAdd(requestData)

            CallbackCommands.CATEGORY_DELETE.isMatch(callbackData) ->
                clickDelete(requestData)

            CallbackCommands.CATEGORY_PAGE.isMatch(callbackData) ->
                clickPage(requestData, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CHOOSE.isMatch(callbackData) ->
                clickChoose(requestData, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CONFIRM.isMatch(callbackData) ->
                clickConfirm(requestData, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_INPUT_CANCEL.isMatch(callbackData) ->
                clickInputCancel(requestData)

            CallbackCommands.CATEGORY_IS_UNREMOVABLE.isMatch(callbackData) ->
                clickIsUnremovable(requestData, CallbackCommands.params(callbackData))
        }
        return requestData.userInfo
    }

    private fun clickActionMenu(data: RequestData, params: List<String>) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_ACTION_CHOOSING,
        )
        if (params[0].toLong() == 1L) {
            editMessage(
                data,
                null,
            )
            sendMessage(
                data,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        } else {
            editMessage(
                data,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        }
    }

    private fun clickChooseMenu(data: RequestData, params: List<String>) {
        val page = params[0].toLong()
        val msgText = when (data.userInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_EDITING ->
                "✏️ Выберите категорию для изменения"

            LastUserActionType.CATEGORY_DELETING ->
                "❌ Выберите категорию для удаления"

            else -> return
        }
        editMessage(
            data,
            msgText,
            CategoryKeyboards.choosingCategory(
                page,
                pageSize,
                categoryRepository,
            ),
        )
    }

    private fun clickEdit(data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_EDITING,
        )
        editMessage(
            data,
            "✏️ Выберите категорию для изменения",
            CategoryKeyboards.choosingCategory(
                0L,
                pageSize,
                categoryRepository,
            ),
        )
    }

    private fun clickAdd(data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_ADDING,
        )
        clickConfirm(data, listOf("0"))
    }

    private fun clickDelete(data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_DELETING,
        )
        editMessage(
            data,
            "❌ Выберите категорию для удаления",
            CategoryKeyboards.choosingCategory(
                0L,
                pageSize,
                categoryRepository,
            ),
        )
    }

    private fun clickPage(data: RequestData, params: List<String>) {
        val page = params[0].toLong()
        editMessage(
            data,
            CategoryKeyboards.choosingCategory(
                page,
                pageSize,
                categoryRepository,
            ),
        )
    }

    private fun clickChoose(data: RequestData, params: List<String>) {
        val catId = params[0].toLong()
        val prevPage = params[1].toLong()
        val category = categoryRepository.findById(catId)
        when (data.userInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_DELETING ->
                editMessage(
                    data,
                    "❓ Вы действительно хотите удалить категорию с\n" +
                        "названием:\t" +
                        "${category.get().title}\n" +
                        "тэгом:\t${category.get().suffix}\n" +
                        "описанием:\t${category.get().description}\n" +
                        "неснимаемая:\t${category.get().isUnremovable}\n",
                    CategoryKeyboards.confirmationAction(catId, prevPage),
                )

            LastUserActionType.CATEGORY_EDITING ->
                editMessage(
                    data,
                    "❓ Вы действительно хотите изменить категорию с\n" +
                        "названием:\t" +
                        "${category.get().title}\n" +
                        "тэгом:\t${category.get().suffix}\n" +
                        "описанием:\t${category.get().description}\n" +
                        "неснимаемая:\t${category.get().isUnremovable}\n",
                    CategoryKeyboards.confirmationAction(catId, prevPage),
                )

            else -> return
        }
    }

    private fun clickConfirm(data: RequestData, params: List<String>) {
        val catId = params[0].toLong()
        when (data.userInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_DELETING ->
                actionDeleteCategory(catId, data)

            LastUserActionType.CATEGORY_EDITING ->
                actionEditCategory(catId, data)

            LastUserActionType.CATEGORY_ADDING ->
                actionAddCategory(data)

            else -> return
        }
    }

    private fun clickInputCancel(data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.DEFAULT,
        )
        val category = categoryRepository.findByChangedByTui(data.userInfo.tui) ?: return
        categoryRepository.deleteById(category.id)
        clickActionMenu(data, listOf("0"))
    }

    private fun clickIsUnremovable(data: RequestData, params: List<String>) {
        val category = categoryRepository.findByChangedByTui(data.userInfo.tui) ?: return
        val isUnremovable = params[0].toLong() == 1L
        categoryRepository.save(
            Category(
                id = category.id,
                title = category.title,
                suffix = category.suffix,
                description = category.description,
                isUnremovable = isUnremovable,
                changedByTui = null,
            ),
        )
        editMessage(
            data,
            CategoryKeyboards.inputCancel(),
        )
        sendMessage(
            data,
            "✅ Категория #${category.suffix} успешно добавлена",
            CategoryKeyboards.choosingAction(),
        )
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.DEFAULT,
        )
    }

    private fun actionDeleteCategory(catId: Long, data: RequestData) {
        val category = categoryRepository.findById(catId)
        if (category.get().changedByTui == null) {
            categoryRepository.deleteById(catId)
            editMessage(
                data,
                keyboard = null,
            )
            sendMessage(
                data,
                "✅ Категория #${category.get().suffix} успешно удалена",
                CategoryKeyboards.confirmationDone(),
            )
        } else {
            editMessage(
                data,
                keyboard = null,
            )
            sendMessage(
                data,
                "❌ Категорию #${category.get().suffix} удалить не получилось, " +
                    "тк сейчас ее именяет другой пользователь",
                CategoryKeyboards.confirmationDone(),
            )
        }
    }

    private fun actionEditCategory(catId: Long, data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_INPUT_START,
        )
        categoryRepository.save(
            Category(
                id = catId,
                changedByTui = data.userInfo.tui,
            ),
        )
        editMessage(
            data,
            keyboard = null,
        )
        sendMessage(
            data,
            "✏️ Введите заголовок категории (до 64 символов):",
            CategoryKeyboards.inputCancel(),
        )
    }

    private fun actionAddCategory(data: RequestData) {
        data.userInfo = data.userInfo.copy(
            lastUserActionType = LastUserActionType.CATEGORY_INPUT_START,
        )
        if (categoryRepository.findByChangedByTui(data.userInfo.tui) == null) {
            categoryRepository.save(
                Category(
                    changedByTui = data.userInfo.tui,
                ),
            )
        }
        editMessage(
            data,
            "✅ Введите заголовок категории (до 64 символов):",
            CategoryKeyboards.inputCancel(),
        )
    }

    private fun sendMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup) {
        val msg = SendMessage(data.chatId, text)
        msg.replyMarkup = keyboard
        bot.execute(msg).messageId
    }

    private fun editMessage(data: RequestData, text: String, keyboard: InlineKeyboardMarkup?) {
        val msgId = data.update.callbackQuery.message.messageId
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

    private fun editMessage(data: RequestData, keyboard: InlineKeyboardMarkup?) {
        val msgId = data.update.callbackQuery.message.messageId
        bot.execute(
            EditMessageReplyMarkup(
                data.chatId,
                msgId,
                null,
                keyboard,
            ),
        )
    }
}
