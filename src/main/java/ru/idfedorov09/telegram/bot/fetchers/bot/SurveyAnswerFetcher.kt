package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class SurveyAnswerFetcher (

) : DefaultFetcher() {
    @InjectData
    @FetcherPerms(UserRole.MAILER)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val params = Params(userActualizedInfo, update)
        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> return
        }
    }

    private fun textCommandsHandler(params: Params) {

    }

    private fun callbackQueryHandler(params: Params) {

    }

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}