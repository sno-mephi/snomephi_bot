package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.QuestDialogRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class ActualizeUserInfoFetcher(
    private val bot: Executor,
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val questDialogRepository: QuestDialogRepository,
    private val broadcastRepository: BroadcastRepository,
) : DefaultFetcher() {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ActualizeUserInfoFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
        expContainer: ExpContainer,
    ): UserActualizedInfo? {
        val tgUser = updatesUtil.getUser(update)
        tgUser ?: run {
            expContainer.byUser = false
            return null
        }
        val tui = tgUser.id.toString()
        // если не нашли в бд пользователя то сохраняем специального

        expContainer.isPersonal = updatesUtil.getChatId(update) == tui

        val userDataFromDatabase =
            userRepository.findByTui(tui)
                ?: User(
                    tui = tgUser.id.toString(),
                    lastTgNick = tgUser.userName,
                    roles = mutableSetOf(UserRole.USER),
                    isRegistered = false,
                    currentKeyboardType = UserKeyboardType.WITHOUT_KEYBOARD, // изачально без выбранной клавиатуры
                ).apply {
                    if (tui == "920061911" || tui == "731119845" || tui == "473458128") {
                        roles.add(UserRole.ROOT)
                    }
                }.let { userRepository.save(it) }

        val categories = categoryRepository.findAllById(userDataFromDatabase.categories).toMutableSet()
        val activeQuest =
            userDataFromDatabase.questDialogId
                ?.let { questDialogRepository.findById(it).getOrNull() }
                ?.let { if (it.questionStatus == QuestionStatus.DIALOG) it else null }

        // обновляем ник в бдшке
        if (userDataFromDatabase.lastTgNick != tgUser.userName) {
            userRepository.save(userDataFromDatabase.copy(lastTgNick = tgUser.userName))
        }

        // Ищем дату о последнем создаваемом репозитории
        val bcData =
            userDataFromDatabase.id?.let {
                broadcastRepository.findLatestUnbuiltBroadcastByAuthor(it)
            }

        userDataFromDatabase.apply {
            return UserActualizedInfo(
                id = id,
                tui = tui,
                lastTgNick = tgUser.userName,
                fullName = fullName,
                studyGroup = studyGroup,
                categories = categories,
                roles = roles,
                lastUserActionType = lastUserActionType,
                activeQuestDialog = activeQuest,
                data = data,
                isRegistered = isRegistered,
                bcData = bcData,
            )
        }
    }
}
