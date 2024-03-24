package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер, сохраняющий обновленные данные; выполняется в конце графа
 */
@Component
class UpdateDataFetcher(
    private val userRepository: UserRepository,
    private val questRepository: QuestRepository,
    private val broadcastRepository: BroadcastRepository,
    private val bot: Executor
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        userActualizedInfo: UserActualizedInfo?,
        update: Update,
    ) {
        if (update.callbackQuery != null) {
            bot.execute(AnswerCallbackQuery(update.callbackQuery.id))
        }
        when {
            userActualizedInfo != null -> updateUser(userActualizedInfo)
        }
    }

    private fun updateUser(userActualizedInfo: UserActualizedInfo) {
        userActualizedInfo.apply {
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
                    questDialogId = getQuestDialogId(activeQuest),
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

    private fun getQuestDialogId(activeQuest: Quest?): Long? {
        val quest = activeQuest?.id?.let { questRepository.findById(it).getOrNull() } ?: return null
        if (quest.questionStatus == QuestionStatus.DIALOG) return quest.id
        return null
    }
}
