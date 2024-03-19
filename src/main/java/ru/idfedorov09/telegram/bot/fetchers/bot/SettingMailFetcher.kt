package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.service.CategoryUpdateService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData

/**
 * Фетчер настраивает рассылку пользователя
 */

@Component
class SettingMailFetcher(
    private val categoryRepository: CategoryRepository,
    private val categoryUpdateService: CategoryUpdateService,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ): UserActualizedInfo {
        if (!(update.hasMessage() && update.message.hasText())) return userActualizedInfo
        val messageText = update.message.text
        val userCategories = userActualizedInfo.categories
        messageText.apply {
            when {
                startsWith(TextCommands.TOGGLE.commandText) -> toggle(messageText, userActualizedInfo)
                equals(TextCommands.SETTING_MAIL.commandText) -> sendSettingMessage(userActualizedInfo)
            }
        }

        return userActualizedInfo.copy(
            categories = userCategories,
        )
    }

    private fun sendSettingMessage(userActualizedInfo: UserActualizedInfo) {
        val allCategoriesInfo =
            categoryRepository.findAll().filter { it.isUnremovable == false }.map {
                "<b>• ${it.title}\n</b>" +
                    "<i>${it.description?.let { "$it\n" }}</i>" +
                    if (userActualizedInfo.categories.contains(it)) {
                        "<b>Включено</b>"
                    } else {
                        "<b>Выключено</b>"
                    } + " - /toggle_${it.suffix}"
            }.joinToString(separator = "\n\n") { it }
        val mailText = "<b>Настройка уведомлений</b>\n\nВыберите интересующие вас направления:\n\n$allCategoriesInfo"
        messageSenderService.sendMessage(
            MessageParams(
                chatId = userActualizedInfo.tui,
                text = mailText,
                parseMode = ParseMode.HTML,
            ),
        )
    }

    private fun toggle(
        messageText: String,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = userActualizedInfo.tui
        val categorySuffix = messageText.substringAfter("/toggle_")
        val category = categoryRepository.findBySuffix(categorySuffix) ?: return
        if (userActualizedInfo.lastUserActionType == LastUserActionType.BC_CHANGE_CATEGORIES) return
        if (category.isUnremovable == true) return
        category.id?.let {
            categoryUpdateService.toggleCategory(
                userId = userActualizedInfo.id,
                categoryId = it,
            )
        }
        if (userActualizedInfo.categories.contains(category)) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = chatId,
                    text = "❌Уведомления о мероприятиях ${category.title} выключены.",
                ),
            )
        } else {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = chatId,
                    text = "✅Уведомления о мероприятиях  ${category.title} включены.",
                ),
            )
        }
    }
}
