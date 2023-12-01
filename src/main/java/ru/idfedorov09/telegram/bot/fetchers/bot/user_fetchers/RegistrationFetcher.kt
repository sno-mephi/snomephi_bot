package ru.idfedorov09.telegram.bot.fetchers.bot.user_fetchers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.UserStrings
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import java.lang.Thread.sleep

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
        val user = userRepository.findByTui(tgUser.id.toString()) ?: run {
            userRepository.save(User())
        }
        when {
            user.fullName == null -> {
                userRepository.save(user.copy(fullName = "n/a"))
                bot.execute(
                    SendMessage(
                        chatId,
                        UserStrings.RegistrationStart(),
                    ),
                )
                bot.execute(
                    SendMessage(
                        chatId,
                        UserStrings.FullNameRequest(),
                    ),
                )
            }

            user.fullName == "n/a" -> {
                if (message.text.isValidFullName()) {
                    userRepository.save(user.copy(fullName = message.text))
                    bot.execute(
                        SendMessage().apply {
                            this.chatId = chatId
                            this.text = UserStrings.FullNameConfirmation.format(message.text)
                            this.replyMarkup = createActionsKeyboard("fullName")
                        },
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

            user.studyGroup == null -> {
                userRepository.findByFullNameAndStudyGroup(user.fullName, message.text)?.run {
                    bot.execute(
                        SendMessage(
                            chatId,
                            UserStrings.AlreadyExists.format(user.lastTgNick?:"")
                        )
                    )
                    return
                }

                if (message.text.isValidGroup()) {
                    userRepository.save(user.copy(studyGroup = message.text))
                    bot.execute(
                        SendMessage().apply {
                            this.chatId = chatId
                            this.text = UserStrings.GroupConfirmation.format(message.text)
                            this.replyMarkup = createActionsKeyboard("studyGroup")
                        },
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
                        UserStrings.Welcome.format(user.fullName),
                    ),
                )
                exp.isUserRegistered = true
            }
        }
    }

    private fun createActionsKeyboard(
        parameter: String
    ) = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("✅ Подтвердить").also {
                    it.callbackData = CallbackCommands.USER_CONFIRM.data.format(parameter)
                },
                InlineKeyboardButton("❌ Отменить").also {
                    it.callbackData = CallbackCommands.USER_DECLINE.data.format(parameter)
                }
            ),
        )
    )

    private fun String?.isValidFullName() = this?.let {
        it.isNotEmpty() && it.length < 128
    } ?: false

    private fun String?.isValidGroup() = this?.let {
        it.isNotEmpty() && "([АМСБамсб]{1})([0-9]{2})-([0-9]{3})".toRegex().matches(it)
    } ?: false

}
