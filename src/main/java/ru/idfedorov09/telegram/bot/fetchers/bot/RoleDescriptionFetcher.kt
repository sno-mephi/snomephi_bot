package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData

/**
 * Фетчер, обрабатывающий команду /role
 * присылает список ролей
 */
@Component
class RoleDescriptionFetcher(
    private val updatesUtil: UpdatesUtil,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {
    @InjectData
    @FetcherPerms(UserRole.ROOT)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update) ?: return

        if (!TextCommands.ROLE_DESCRIPTION.commandText.equals(messageText)) return

        if (!TextCommands.ROLE_DESCRIPTION.isAllowed(userActualizedInfo)) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = chatId,
                    text = "Нет прав",
                ),
            )

            return
        }

        val text =
            UserRole.entries.joinToString("\n\n") {
                "- ${it}\n\t${it.description}"
            }

        messageSenderService.sendMessage(
            MessageParams(
                chatId = chatId,
                text = text,
            ),
        )
    }
}
