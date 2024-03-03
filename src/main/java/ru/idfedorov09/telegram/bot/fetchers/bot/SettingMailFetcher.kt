package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.Category
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер настраивает рассылку пользователя
 */

@Component
class SettingMailFetcher(
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val bot: Executor,

) : GeneralFetcher() {
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
                startsWith(TextCommands.TOGGLE.commandText) -> toggle(messageText, userActualizedInfo, userCategories)
                equals(TextCommands.SETTING_MAIL.commandText) -> sendSettingMessage(userActualizedInfo)
            }
        }

        return userActualizedInfo.copy(
            categories = userCategories,
        )
    }

    private fun sendSettingMessage(userActualizedInfo: UserActualizedInfo) {
        val allCategoriesInfo = categoryRepository.findAll().filter{it.isUnremovable == false }.map {
            "<b>• ${it.title}\n</b>" +
                "<i>${it.description?.let { "$it\n" }}</i>" +
                if (userActualizedInfo.categories.contains(it)) {
                    "<b>Включено</b>"
                } else {
                    "<b>Выключено</b>"
                } + " - /toggle_${it.suffix}"
        }.joinToString(separator = "\n") { it }
        val mailText = "<b>Настройка уведомлений</b>\n\nВыберите интересующие вас направления:\n\n$allCategoriesInfo"
        bot.execute(
            SendMessage().also {
                it.chatId = userActualizedInfo.tui
                it.text = mailText
                it.parseMode = ParseMode.HTML
            },
        )
    }

    private fun toggle(
        messageText: String,
        userActualizedInfo: UserActualizedInfo,
        userCategories: MutableSet<Category>,
    ) {
        val chatId = userActualizedInfo.tui
        val categorySuffix = messageText.substringAfter("/toggle_")
        val category = categoryRepository.findBySuffix(categorySuffix) ?: return
        if (category.isUnremovable == true) return
        if (userActualizedInfo.categories.contains(category)) {
            userCategories.remove(category)
            bot.execute(
                SendMessage().also {
                    it.chatId = chatId
                    it.text = "❌Уведомления о мероприятиях ${category.title} выключены."
                },
            )
        } else {
            userCategories.add(category)
            bot.execute(
                SendMessage().also {
                    it.chatId = chatId
                    it.text = "✅Уведомления о мероприятиях  ${category.title} включены.."
                },
            )
        }
    }
}