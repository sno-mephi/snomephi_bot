package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class UserSettingFetcher(
    private val callbackDataRepository: CallbackDataRepository
): DefaultFetcher() {
    @InjectData
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
        val text = params.update.message.text
        text.apply {
            when {
                startsWith(TextCommands.USER_SETTING.commandText) -> showUserInfo(params)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun commonTextHandler(params: Params) {

    }

    private fun showUserInfo(params: Params) {

    }

    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                CallbackCommands.SETTING_USER_CHANGE_FULL_NAME.isMatch(this) -> changeFullName(params)
                CallbackCommands.SETTING_USER_CHANGE_STUDY_GROUP.isMatch(this) -> changeStudyGroup(params)
                else -> return
            }
        }
    }

    private fun changeStudyGroup(params: Params) {

    }

    private fun changeFullName(params: Params) {

    }


    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}