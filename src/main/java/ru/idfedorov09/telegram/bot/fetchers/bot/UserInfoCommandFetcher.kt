package ru.idfedorov09.telegram.bot.fetchers.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер, обрабатывающий команду /userInfo
 * присылает полную информацию о пользователе
 */
@Component
class UserInfoCommandFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
    private val bot: Executor,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(UserInfoCommandFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update) ?: return

        val params = UserInfoCommandFetcher.Params(
            messageText = messageText,
            userActualizedInfo = userActualizedInfo,
            update = update,
            chatId = chatId,
        )

        if (messageText.contains(TextCommands.USER_INFO.commandText)) {
            handleCommands(params)
        }
    }

    private fun handleCommands(params: UserInfoCommandFetcher.Params) {
        if (!params.messageText.contains(TextCommands.USER_INFO.commandText)) return

        if (!TextCommands.USER_INFO.isAllowed(params.userActualizedInfo)) {
            bot.execute(SendMessage(params.chatId, "Нет прав"))
            return
        }

        val messageSplit = params.messageText.split(" ", "\n", "\t")
        when (messageSplit.size) {
            1 -> bot.execute(
                SendMessage(
                    params.chatId,
                    "отправьте команду формата" +
                        " \"/userinfo tui\"",
                ),
            )
            else -> {
                val tui = messageSplit[1]
                sendUserInfo(params, tui)
            }
        }
    }

    private fun sendUserInfo(params: Params, tui: String) {
        val user = userRepository.findByTui(tui)
        if (user == null) {
            bot.execute(SendMessage(params.chatId, "Пользователь не найден"))
            return
        }

        val fullName = user.fullName ?: "-"
        val studyGroup = user.studyGroup ?: "-"
        val lastTgNick = user.lastTgNick ?: "-"
        val id = user.id ?: "-"
        val lastUserActionType = user.lastUserActionType ?: "-"

        var userCategories = ""
        if (user.categories.isNotEmpty()) {
            for (s in user.categories) {
                userCategories += String.format("\n-%s", s)
            }
        } else {
            userCategories = "\nпусто"
        }

        var userRoles = ""
        if (user.roles.isNotEmpty()) {
            for (s in user.roles) {
                userRoles += String.format("\n-%s", s)
            }
        } else {
            userRoles = "\nпусто"
        }

        val msg = SendMessage()
        msg.chatId = params.chatId
        msg.text = "\uD83D\uDC64ФИО: $fullName\n\uD83D\uDCDAгруппа: $studyGroup\n\n\uD83D\uDD11роли:$userRoles\n\n" +
            "\uD83D\uDCF1последний ник в tg: $lastTgNick\n\n\uD83D\uDDD2категории:$userCategories\n\ntui: $tui\n" +
            "id: $id\n\nпоследнее действие: $lastUserActionType"
        bot.execute(msg)
    }

    private data class Params(
        val messageText: String,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        val chatId: String,
    )
}
