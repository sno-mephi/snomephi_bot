package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.model.QuestDialog
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер, сохраняющий обновленные данные; выполняется в конце графа
 */
@Component
class UpdateDataFetcher(
    private val userRepository: UserRepository,
    private val questDialogRepository: QuestDialogRepository,
    private val broadcastRepository: BroadcastRepository,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(userActualizedInfo: UserActualizedInfo?) {
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

    private fun getQuestDialogId(activeQuestDialog: QuestDialog?): Long? {
        val quest = activeQuestDialog?.id?.let { questDialogRepository.findById(it).getOrNull() } ?: return null
        if (quest.questionStatus == QuestionStatus.DIALOG) return quest.id
        return null
    }
}
