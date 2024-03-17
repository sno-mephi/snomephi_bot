package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
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
    private val messageSenderService: MessageSenderService,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = updatesUtil.getText(update) ?: return

        val params =
            Params(
                messageText = messageText,
                userActualizedInfo = userActualizedInfo,
                update = update,
                chatId = chatId,
            )

        if (messageText.contains(TextCommands.USER_INFO.commandText)) {
            handleCommands(params)
        }
    }

    private fun handleCommands(params: Params) {
        if (!TextCommands.USER_INFO.isAllowed(params.userActualizedInfo)) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = "Нет прав",
                ),
            )
            return
        }

        val tui: String? =
            Regex("""${TextCommands.USER_INFO.commandText}\s+\d+""")
                .find(params.messageText)?.value?.let { Regex("""\d+""").find(it)?.value }

        if (tui == null) {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = "отправьте команду формата\n\"/userInfo tui\"",
                ),
            )
            return
        }

        sendUserInfo(params, tui)
    }

    private fun sendUserInfo(
        params: Params,
        tui: String,
    ) {
        val user = userRepository.findByTui(tui)
        if (user == null) {
            bot.execute(SendMessage(params.chatId, "Пользователь не найден"))
            return
        }

        user.apply {
            val userCategories =
                if (categories.isNotEmpty()) {
                    "\n" + categories.map { categoryRepository.findById(it).get().suffix }.joinToString(separator = ", ")
                } else {
                    "\nпусто"
                }
            val userRoles =
                if (roles.isNotEmpty()) {
                    "\n" + roles.joinToString(separator = ", ")
                } else {
                    "\nпусто"
                }
            val userNick =
                if (lastTgNick == null) {
                    "-"
                } else {
                    "@{lastTgNick}"
                }
            val msgText =
                "\uD83D\uDC64ФИО: ${fullName ?: "-"}\n\uD83D\uDCDAгруппа: ${studyGroup ?: "Не из МИФИ"}" +
                    "\n\n\uD83D\uDD11роли:${userRoles ?: "-"}\n\n\uD83D\uDCF1последний ник в tg: " +
                    "$userNick}\n\n\uD83D\uDDD2категории:${userCategories ?: "-"}\n\ntui: ${tui ?: "-"}\n" +
                    "id: ${id ?: "-"}\n\nпоследнее действие: ${lastUserActionType ?: "-"}"
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = params.chatId,
                    text = msgText,
                ),
            )
        }
    }

    private data class Params(
        val messageText: String,
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        val chatId: String,
    )
}
