package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants.MAX_CATEGORY_COUNTS
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.keyboards.CategoryKeyboards
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData

/**
 * Фетчер, обрабатывающий нажатия на кнопки категорий
 */
@Component
class CategoryButtonHandlerFetcher(
    private val updatesUtil: UpdatesUtil,
    private val messageSenderService: MessageSenderService,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
) : DefaultFetcher() {
    private data class Params(
        val chatId: String,
        val update: Update,
        var userActualizedInfo: UserActualizedInfo,
    )

    companion object {
        private val PAGESIZE: Long = 6
    }

    @InjectData
    @FetcherPerms(UserRole.CATEGORY_BUILDER)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (update.callbackQuery == null) return userActualizedInfo
        val callbackData = update.callbackQuery.data
        val chatId = updatesUtil.getChatId(update) ?: return userActualizedInfo
        val params =
            Params(
                chatId,
                update,
                userActualizedInfo,
            )
        when {
            CallbackCommands.CATEGORY_ACTION_MENU.isMatch(callbackData) ->
                clickActionMenu(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CHOOSE_MENU.isMatch(callbackData) ->
                clickChooseMenu(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_EDIT.isMatch(callbackData) ->
                clickEdit(params)

            CallbackCommands.CATEGORY_ADD.isMatch(callbackData) ->
                clickAdd(params)

            CallbackCommands.CATEGORY_DELETE.isMatch(callbackData) ->
                clickDelete(params)

            CallbackCommands.CATEGORY_PAGE.isMatch(callbackData) ->
                clickPage(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CHOOSE.isMatch(callbackData) ->
                clickChoose(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_CONFIRM.isMatch(callbackData) ->
                clickConfirm(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_INPUT_CANCEL.isMatch(callbackData) ->
                clickInputCancel(params)

            CallbackCommands.CATEGORY_IS_UNREMOVABLE.isMatch(callbackData) ->
                clickIsUnremovable(params, CallbackCommands.params(callbackData))

            CallbackCommands.CATEGORY_EXIT.isMatch(callbackData) ->
                clickExit(params)
        }
        return params.userActualizedInfo
    }

    private fun clickActionMenu(
        params: Params,
        callBackParams: List<String>,
    ) {
        if (callBackParams[0].toLong() == 1L) {
            editMessage(
                params,
                null,
            )
            sendMessage(
                params,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        } else {
            editMessage(
                params,
                "⬇️ Выберите действие",
                CategoryKeyboards.choosingAction(),
            )
        }
    }

    private fun clickChooseMenu(
        params: Params,
        callBackParams: List<String>,
    ) {
        val page = callBackParams[0].toLong()
        val msgText =
            when (params.userActualizedInfo.lastUserActionType) {
                LastUserActionType.CATEGORY_EDITING ->
                    "✏️ Выберите категорию для изменения"

                LastUserActionType.CATEGORY_DELETING ->
                    "❌ Выберите категорию для удаления"

                else -> return
            }
        editMessage(
            params,
            msgText,
            CategoryKeyboards.choosingCategory(
                page,
                PAGESIZE,
                categoryRepository,
            ),
        )
    }

    private fun clickEdit(params: Params) {
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_EDITING,
            )
        editMessage(
            params,
            "✏️ Выберите категорию для изменения",
            CategoryKeyboards.choosingCategory(
                0L,
                PAGESIZE,
                categoryRepository,
            ),
        )
    }

    private fun clickAdd(params: Params) {
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_ADDING,
            )
        clickConfirm(params, listOf("0"))
    }

    private fun clickDelete(params: Params) {
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_DELETING,
            )
        editMessage(
            params,
            "❌ Выберите категорию для удаления",
            CategoryKeyboards.choosingCategory(
                0L,
                PAGESIZE,
                categoryRepository,
            ),
        )
    }

    private fun clickPage(
        params: Params,
        callBackParams: List<String>,
    ) {
        val page = callBackParams[0].toLong()
        editMessage(
            params,
            CategoryKeyboards.choosingCategory(
                page,
                PAGESIZE,
                categoryRepository,
            ),
        )
    }

    private fun clickChoose(
        params: Params,
        callBackParams: List<String>,
    ) {
        val catId = callBackParams[0].toLong()
        val prevPage = callBackParams[1].toLong()
        val category = categoryRepository.findById(catId)
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_DELETING ->
                editMessage(
                    params,
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
                    params,
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

    private fun clickConfirm(
        params: Params,
        callBackParams: List<String>,
    ) {
        val catId = callBackParams[0].toLong()
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.CATEGORY_DELETING ->
                actionDeleteCategory(catId, params)

            LastUserActionType.CATEGORY_EDITING ->
                actionEditCategory(catId, params)

            LastUserActionType.CATEGORY_ADDING ->
                actionAddCategory(params)

            else -> return
        }
    }

    private fun clickInputCancel(params: Params) {
        val category = categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) ?: return
        category.id?.let { categoryRepository.deleteById(it) }
        clickActionMenu(params, listOf("0"))
    }

    private fun clickIsUnremovable(
        params: Params,
        callBackParams: List<String>,
    ) {
        val category = categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) ?: return
        val isUnremovable = callBackParams[0].toLong() == 0L
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

        messageSenderService.editMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = params.userActualizedInfo.data?.categoryMessageId,
                text = "✅ Категория #${category.suffix} успешно добавлена",
                replyMarkup = CategoryKeyboards.confirmationDone(),
            ),
        )
        // TODO: Пока что нет логики, которая делает isSetupByDefault = false
        if (category.isSetupByDefault) {
            category.id?.let { userRepository.addCategoryForAllUser(it) }
        }
    }

    private fun actionDeleteCategory(
        catId: Long,
        params: Params,
    ) {
        val category = categoryRepository.findById(catId)
        if (category.get().changedByTui == null) {
            categoryRepository.deleteById(catId)
            editMessage(
                params,
                keyboard = null,
            )
            sendMessage(
                params,
                "✅ Категория #${category.get().suffix} успешно удалена",
                CategoryKeyboards.confirmationDone(),
            )
        } else {
            editMessage(
                params,
                keyboard = null,
            )
            sendMessage(
                params,
                "❌ Категорию #${category.get().suffix} удалить не получилось, " +
                    "так сейчас ее именяет другой пользователь",
                CategoryKeyboards.confirmationDone(),
            )
        }
    }

    private fun actionEditCategory(
        catId: Long,
        params: Params,
    ) {
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_INPUT_START,
            )
        categoryRepository.save(
            Category(
                id = catId,
                changedByTui = params.userActualizedInfo.tui,
            ),
        )
        editMessage(
            params,
            keyboard = null,
        )
        sendMessage(
            params,
            "✏️ Введите заголовок категории (до 64 символов):",
            CategoryKeyboards.inputCancel(),
        )
    }

    private fun actionAddCategory(params: Params) {
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.CATEGORY_INPUT_START,
            )
        if (categoryRepository.findByChangedByTui(params.userActualizedInfo.tui) == null) {
            categoryRepository.save(
                Category(
                    changedByTui = params.userActualizedInfo.tui,
                ),
            )
        }

        if (categoryRepository.categoryCount() > MAX_CATEGORY_COUNTS) {
            editMessage(
                params,
                "❗Превышен лимит категорий (>${MAX_CATEGORY_COUNTS})",
                CategoryKeyboards.inputCancel(),
            )
            params.userActualizedInfo.lastUserActionType = LastUserActionType.CATEGORY_ADDING
            return
        } else {
            editMessage(
                params,
                "✏️Введите заголовок категории (до 64 символов):",
                CategoryKeyboards.inputCancel(),
            )
        }
    }

    private fun clickExit(params: Params) {
        params.userActualizedInfo.data?.categoryMessageId?.let {
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = params.chatId,
                    messageId = it,
                ),
            )
        }
        params.userActualizedInfo =
            params.userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.DEFAULT,
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

    private fun editMessage(
        params: Params,
        text: String,
        keyboard: InlineKeyboardMarkup?,
    ) {
        val msgId = params.update.callbackQuery.message.messageId
        messageSenderService.editMessage(
            MessageParams(
                chatId = params.chatId,
                messageId = msgId,
                text = text,
                replyMarkup = keyboard,
            ),
        )
        params.userActualizedInfo.data?.categoryMessageId = msgId
    }

    private fun editMessage(
        params: Params,
        keyboard: InlineKeyboardMarkup?,
    ) {
        val msgId = params.update.callbackQuery.message.messageId
        messageSenderService.editMessageReplyMarkup(
            MessageParams(
                chatId = params.chatId,
                messageId = msgId,
                replyMarkup = keyboard,
            ),
        )
        params.userActualizedInfo.data?.categoryMessageId = msgId
    }
}
