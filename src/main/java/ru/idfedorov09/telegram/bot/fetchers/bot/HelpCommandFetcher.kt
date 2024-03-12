package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.service.BroadcastSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
/**
фетчер для рассылки команд /help
 */
@Component
class HelpCommandFetcher(
) : GeneralFetcher() {


    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
        bot: Executor,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return
        val messageText = update.message.text

        if (messageText.startsWith(TextCommands.HELP_COMMAND.commandText))
        {
            val textForUser = listOf(
                "/help - показывает все доступные вам команды",
                "Мероприятия недели - присылает информацию о всех мероприятиях, запланированных на текущую неделю",
                "Настройка уведомлений - помогает настроить рассылку нужных вам уведомлений о мероприятих и кружках")

            val textForMailer = listOf("Рассылка уведомлений - открывает конструктор рассылки уведомлений для дальнейшей настройки")

            val textForCategoryBuilder = listOf("/category - настройка категорий")

            val textForRoot = listOf(
                "/userInfo - присылает полную информацию о пользователе",
                "/role - присылает полный список ролей пользователя")

            var finalText = textForUser.joinToString(separator = "\n▪",prefix ="▪",postfix = "\n")

            if (UserRole.ROOT in userActualizedInfo.roles){
                finalText += textForCategoryBuilder.joinToString(separator = "\n▪",prefix ="▪", postfix = "\n") + textForMailer.joinToString(separator = "\n▪",prefix ="▪",postfix = "\n") + textForRoot.joinToString(separator = "\n▪",prefix ="▪",postfix = "\n")
            }
            else{
            for(role in userActualizedInfo.roles){
                when(role){
                    UserRole.CATEGORY_BUILDER -> finalText += textForCategoryBuilder.joinToString(separator = "\n▪",prefix ="▪", postfix = "\n")
                    UserRole.MAILER -> finalText += textForMailer.joinToString(separator = "\n▪",prefix ="▪",postfix = "\n")
                    else -> {}
                }
            }
            }

            bot.execute(
                SendMessage().also {
                    it.chatId = userActualizedInfo.tui
                    it.text = finalText
                    it.parseMode = ParseMode.HTML
                },
            )
        }
    }
}