
package ru.idfedorov09.telegram.bot.fetchers.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class TestFetcher(
    private val updatesUtil: UpdatesUtil,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(TestFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: Executor,
    ) {
        val msg = updatesUtil.getText(update)
        bot.execute(
            SendMessage(
                updatesUtil.getChatId(update)!!,
                "ok!!",
            ),
        )
        // пример проверки является ли текст командой
        msg?.let {
            if (TextCommands.isTextCommand(msg)) {
                bot.execute(
                    SendMessage(
                        updatesUtil.getChatId(update)!!,
                        "да, это команда)",
                    ),
                )
            }
        }
    }
}
