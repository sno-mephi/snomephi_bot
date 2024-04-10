package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserAction
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.UserActionRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер, сохраняющий обновленные данные; выполняется в конце графа
 */
@Component
class UpdateDataFetcher(
    private val userRepository: UserRepository,
    private val questDialogRepository: QuestDialogRepository,
    private val broadcastRepository: BroadcastRepository,
    private val userActionRepository: UserActionRepository,
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo?,
        update: Update,
    ) {
        if (update.callbackQuery != null) {
            bot.execute(AnswerCallbackQuery(update.callbackQuery.id))
        }
        when {
            userActualizedInfo != null -> updateUser(userActualizedInfo, update)
        }
    }

    private fun updateUser(userActualizedInfo: UserActualizedInfo, update: Update) {
        userActualizedInfo.apply {
            val lastUserActionTypeFromRepository = id?.let { userRepository.findById(it).get().lastUserActionType }
            if (lastUserActionTypeFromRepository != lastUserActionType) newUserAction(userActualizedInfo, update)
            userRepository.save(
                User(
                    id = id,
                    tui = tui,
                    lastTgNick = lastTgNick,
                    fullName = fullName,
                    studyGroup = studyGroup,
                    categories = categories.mapNotNull { it.id }.toMutableSet(),
                    roles = roles,
                    lastUserActionType = lastUserActionType,
                    questDialogId = getQuestDialogId(activeQuestDialog),
                    data = data,
                    isRegistered = isRegistered,
                    constructorId = bcData?.id,
                    isDeleted = isDeleted,
                ),
            )

            bcData?.let {
                broadcastRepository.save(it)
            }
        }
    }

    private fun newUserAction(userActualizedInfo: UserActualizedInfo, update: Update) {
        userActualizedInfo.apply {
            val callbackId = if (update.callbackQuery?.data?.contains(Regex("\"\\\\d\"")) == true) {
                update.callbackQuery?.data!!.toLong()
            } else null
            userActionRepository.save(
                UserAction(
                    lastUserActionType = lastUserActionType,
                    actionTime = updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() },
                    userId = id,
                    tui = tui,
                    messageText = update.message?.text,
                    callbackDataId = callbackId,
                    callbackDataLegacy = callbackId?.let { _ ->
                        update.callbackQuery?.data
                    }
                )
            )
        }
    }

    private fun getQuestDialogId(activeQuestDialog: QuestDialog?): Long? {
        val quest = activeQuestDialog?.id?.let { questDialogRepository.findById(it).getOrNull() } ?: return null
        if (quest.questionStatus == QuestionStatus.DIALOG) return quest.id
        return null
    }
}
