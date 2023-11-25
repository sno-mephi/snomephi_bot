package ru.idfedorov09.telegram.bot.fetchers.bot.registrationFetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.config.*
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.util.isValidFullName
import ru.idfedorov09.telegram.bot.util.isValidGroup
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class RegistrationFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(RegistrationFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        exp: ExpContainer,
    ) {
        val chatId = updatesUtil.getChatId(update) ?: return
        val messageText = update.message.text

        // если пользователя нет в бд, то создаем пустого
        val user = userRepository.findByTui(chatId) ?: run {
            bot.execute(
                SendMessage(
                    chatId,
                    REGISTRATION_START,
                ),
            )
            userRepository.save(User())
        }

        exp.byUser = false
        when (null) {
            user.tui -> {
                userRepository.save(user.copy(tui = chatId))
                bot.execute(
                    SendMessage(
                        chatId,
                        REGISTRATION_FULL_NAME_REQUEST,
                    ),
                )
            }

            user.fullName -> {
                if (messageText.isValidFullName()) {
                    userRepository.save(user.copy(fullName = messageText))
                    bot.execute(
                        SendMessage(
                            chatId,
                            REGISTRATION_GROUP_REQUEST,
                        ),
                    )
                } else {
                    bot.execute(
                        SendMessage(
                            chatId,
                            REGISTRATION_INVALID_FULL_NAME,
                        ),
                    )
                }
            }

            user.studyGroup -> {
                if (messageText.isValidGroup()) {
                    userRepository.save(user.copy(studyGroup = messageText))
                    bot.execute(
                        SendMessage(
                            chatId,
                            REGISTRATION_WELCOME + ", ${user.fullName} из группы $messageText",
                        ),
                    )
                } else {
                    bot.execute(
                        SendMessage(
                            chatId,
                            REGISTRATION_INVALID_GROUP,
                        ),
                    )
                }
            }

            else -> {
                bot.execute(
                    SendMessage(
                        chatId,
                        REGISTRATION_WELCOME + " ${user.fullName} из группы ${user.studyGroup}",
                    ),
                )
                exp.byUser = true
                return
            }
        }
    }
}
