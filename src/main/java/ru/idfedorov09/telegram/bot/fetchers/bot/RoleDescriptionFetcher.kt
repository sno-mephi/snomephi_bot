package ru.idfedorov09.telegram.bot.fetchers.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RoleDescriptionFetcher(
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(RoleDescriptionFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update) ?: return

        if (TextCommands.isTextCommand(messageText)) {
            if (!messageText.contains(TextCommands.ROLE_DESCRIPTION.commandText)) return

            if (TextCommands.ROLE_DESCRIPTION.isAllowed(userActualizedInfo)) {
                var text = ""
                for (s in UserRole.values()) {
                    text += "-" + s.roleName + "\n\t" + s.roleDescription + "\n\n"
                }
                bot.execute(SendMessage(chatId, text))
            } else {
                bot.execute(SendMessage(chatId, "Нет прав"))
            }
        }
    }
}
