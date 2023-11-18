package ru.idfedorov09.telegram.bot.fetchers.bot.registrationFetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.config.*
import ru.idfedorov09.telegram.bot.data.enums.stages.BotStage.*
import ru.idfedorov09.telegram.bot.data.enums.stages.RegistrationStage.*
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.domain.use_cases.message.SendMessageUseCase
import ru.idfedorov09.telegram.bot.domain.use_cases.user.CreateUserUseCase
import ru.idfedorov09.telegram.bot.domain.use_cases.user.GetUserUseCase
import ru.idfedorov09.telegram.bot.domain.use_cases.user.UpdateUserUseCase
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.util.isValidFullName
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.system.exitProcess

@Component
class RegistrationFetcher(
    private val updatesUtil: UpdatesUtil,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        exp: ExpContainer
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = update.message.text

        val user = getUserUseCase(chatId) ?: createUserUseCase()

        when (null) {
            user.tui -> {
                updateUserUseCase(user.copy(tui = chatId))
                sendMessageUseCase(bot, REGISTRATION_FULL_NAME_REQUEST, chatId)
            }

            user.fullName -> {
                if (messageText.isValidFullName()) {
                    updateUserUseCase(user.copy(fullName = messageText))
                    sendMessageUseCase(bot, REGISTRATION_GROUP_REQUEST, chatId)
                } else {
                    sendMessageUseCase(bot, REGISTRATION_INVALID_FULL_NAME, chatId)
                }
            }

            user.studyGroup -> {
                if (messageText.isValidFullName()) {
                    updateUserUseCase(user.copy(studyGroup = messageText))
                    sendMessageUseCase(bot, REGISTRATION_WELCOME + " ${user.fullName} из группы $messageText", chatId)
                } else {
                    sendMessageUseCase(bot, REGISTRATION_INVALID_GROUP, chatId)
                }
            }

            else -> {
                sendMessageUseCase(bot, REGISTRATION_WELCOME + " ${user.fullName} из группы ${user.studyGroup}", chatId)
                exp.registrationStage = COMPLETED
                return
            }
        }
    }

}
