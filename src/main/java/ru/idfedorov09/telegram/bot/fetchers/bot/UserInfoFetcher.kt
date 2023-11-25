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

@Component
class UserInfoFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(UserInfoFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update) ?: return

        val params = UserInfoFetcher.Params(
            messageText = messageText,
            userActualizedInfo = userActualizedInfo,
            update = update,
            chatId = chatId,
            bot = bot,
        )

        if (messageText.contains(TextCommands.USER_INFO.commandText)) {
            handleCommands(params)
        }
    }

    private fun handleCommands(params: UserInfoFetcher.Params) {
        if (!params.messageText.contains(TextCommands.USER_INFO.commandText)) return

        if (TextCommands.USER_INFO.commandText in params.messageText) {
            if (TextCommands.USER_INFO.isAllowed(params.userActualizedInfo)) {
                val messageSplit = params.messageText.split(" ", "\n", "\t")
                when (messageSplit.size) {
                    1 -> params.bot.execute(
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
            } else {
                params.bot.execute(SendMessage(params.chatId, "Нет прав"))
            }
        }
    }

    private fun sendUserInfo(params: Params, tui: String) {
        val user = userRepository.findByTui(tui)
        if (user == null) {
            params.bot.execute(SendMessage(params.chatId, "Пользователь не найден"))
            return
        }

        val fullName = user.fullName ?: "-"
        val studyGroup = user.studyGroup ?: "-"
        val lastTgNick = user.lastTgNick ?: "-"
        val id = user.id ?: "-"
        // записать в нормальной форме
        val lastUserActionType = when (user.lastUserActionType) {
            null -> "-"
            else -> user.lastUserActionType.actionDescription
        }

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
                userRoles += String.format("\n-%s", s.roleName)
            }
        } else {
            userRoles = "\nпусто"
        }

        val msg = SendMessage()
        msg.chatId = params.chatId
        msg.text = "\uD83D\uDC64ФИО: $fullName\n\uD83D\uDCDAгруппа: $studyGroup\n\n\uD83D\uDD11роли:$userRoles\n\n" +
            "\uD83D\uDCF1последний ник в tg: $lastTgNick\n\n\uD83D\uDDD2категории:$userCategories\n\ntui: $tui\n" +
            "id: $id\n\nпоследнее действие: $lastUserActionType"
        params.bot.execute(msg)
    }

    private data class Params(
        val messageText: String,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        val chatId: String,
        val bot: Executor,
    )
}
