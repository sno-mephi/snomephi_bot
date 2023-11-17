package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.util.UpdatesUtil
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

@Component
class ActualizeUserInfoFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
) : GeneralFetcher() {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ActualizeUserInfoFetcher::class.java)
    }

    @InjectData
    fun doFetch(
        update: Update,
    ): UserActualizedInfo? {
        val tgUser = updatesUtil.getUser(update)
        tgUser ?: return null
        val tui = tgUser.id.toString()

        // если не нашли в бд пользователя то сохраняем специального
        val userDataFromDatabase = userRepository.findByTui(tui)
            ?: return notRegisteredUserInfo(tgUser)

        val categories = categoryRepository.findAllById(userDataFromDatabase.categories).toMutableSet()

        // обновляем ник в бдшке
        userRepository.save(userDataFromDatabase.copy(lastTgNick = tgUser.userName))

        return UserActualizedInfo(
            id = userDataFromDatabase.id,
            tui = tui,
            lastTgNick = tgUser.userName,
            fullName = userDataFromDatabase.fullName,
            studyGroup = userDataFromDatabase.studyGroup,
            categories = categories,
            roles = userDataFromDatabase.roles,
            lastUserActionType = userDataFromDatabase.lastUserActionType,
        )
    }

    private fun notRegisteredUserInfo(
        tgUser: org.telegram.telegrambots.meta.api.objects.User,
    ): UserActualizedInfo {
        return UserActualizedInfo(
            id = null,
            tui = tgUser.id.toString(),
            lastTgNick = tgUser.userName,
            fullName = null,
            studyGroup = null,
            categories = categoryRepository.findAll().filter { it.isSetupByDefault }.toMutableSet(),
            roles = mutableSetOf(UserRole.USER),
            lastUserActionType = null,
        )
    }
}
