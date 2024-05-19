package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class SurveyAnswerFetcher (
    private val callbackDataRepository: CallbackDataRepository,
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
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.SURVEY_USER_ANSWER.data)
            }
        }
    }

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}