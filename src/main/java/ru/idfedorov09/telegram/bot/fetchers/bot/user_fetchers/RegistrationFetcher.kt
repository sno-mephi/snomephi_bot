package ru.idfedorov09.telegram.bot.fetchers.bot.user_fetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.UserStrings
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
    private val userRepository: UserRepository
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
        val tgUser = updatesUtil.getUser(update) ?: return
        val message = update.message.takeIf { update.hasMessage() } ?: return

        // если пользователя нет в бд, то создаем пустого
        val user = userRepository.findByTui(chatId) ?: run {
            bot.execute(
                SendMessage(
                    chatId,
                    UserStrings.RegistrationStart(),
                ),
            )
            userRepository.save(User())
        }

        exp.byUser = false
        when (null) {
            user.tui -> {
                userRepository.save(
                    user.copy(
                        tui = tgUser.id.toString(),
                        lastTgNick = tgUser.userName
                    )
                )
                bot.execute(
                    SendMessage(
                        chatId,
                        UserStrings.FullNameRequest(),
                    ),
                )
            }

            user.fullName -> {
                if (message.text.isValidFullName()) {
                    userRepository.save(user.copy(fullName = message.text))
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.GroupRequest(),
                        ),
                    )
                } else {
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.InvalidFullName(),
                        ),
                    )
                }
            }

            user.studyGroup -> {
                userRepository.findByFullNameAndStudyGroup(user.fullName, message.text)?.run {
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.AlreadyExists("под юзернеймом @${user.lastTgNick}")
                        )
                    )
                    return
                }

                if (message.text.isValidGroup()) {
                    userRepository.save(user.copy(studyGroup = message.text))
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.RegistrationComplete(),
                        ),
                    )
                } else {
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.InvalidGroup(),
                        ),
                    )
                }
            }

            else -> {
                bot.execute(
                    SendMessage(
                        chatId,
                        UserStrings.Welcome( ", ${user.fullName}"),
                    ),
                )
                exp.byUser = true
                return
            }
        }
    }
}
