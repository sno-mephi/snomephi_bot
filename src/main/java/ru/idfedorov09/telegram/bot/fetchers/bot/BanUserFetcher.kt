package ru.idfedorov09.telegram.bot.fetchers.bot

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.Ban
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BanRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер для бана пользователей
 * **/
class BanUserFetcher(
    private val userRepository: UserRepository,
    private val banRepository: BanRepository,
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {

    companion object {
        private val log = LoggerFactory.getLogger(BanUserFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
    ) {
        val msg = updatesUtil.getText(update)
        msg?.let {
            if (msg == TextCommands.BAN_COMMAND.toString()) {
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
                bot.execute(
                    SendMessage(
                        updatesUtil.getChatId(update)!!,
                        "Напишите причину бана",
                    ),
                )
                val comment = updatesUtil.getText(update)
                comment?.let {
                    val ban: Ban = Ban(
                        userId = user.id,
                        comment = comment,
                    )
                    banRepository.save(ban)

                    bot.execute(
                        SendMessage(
                            updatesUtil.getChatId(update)!!,
                            "Пользователь $nickname забанен",
                        ),
                    )
                }
            }
        }
    }
}
