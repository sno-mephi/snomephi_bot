package ru.idfedorov09.telegram.bot.fetchers.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.Ban
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BanRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class UnbanUserFetcher(
    private val userRepository: UserRepository,
    private val banRepository: BanRepository,
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(UnbanUserFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
        userActualizedInfo: UserActualizedInfo,
    ) {
        if (!TextCommands.UNBAN_COMMAND.isAllowed(userActualizedInfo)) {
            bot.execute(
                SendMessage(
                    updatesUtil.getChatId(update)!!,
                    "Нет прав на выполнение команды",
                ),
            )
            return
        }

        val msg = updatesUtil.getText(update)
        msg?.let {
            if (msg == TextCommands.UNBAN_COMMAND.toString()) {
                bot.execute(
                    SendMessage(
                        updatesUtil.getChatId(update)!!,
                        "Отправте никнейм пользователя",
                    ),
                )
            }
        }
        val nickname = updatesUtil.getText(update)
        nickname?.let {
            val user: User? = userRepository.findByLastTgNick(it)
            if (user == null) {
                bot.execute(
                    SendMessage(
                        updatesUtil.getChatId(update)!!,
                        "Такого пользователя не существует",
                    ),
                )
            } else {
                val ban: Ban? = banRepository.findByUserId(userId = user.id)
                ban?.let {
                    banRepository.delete(ban)
                }
            }
        }
    }
}
