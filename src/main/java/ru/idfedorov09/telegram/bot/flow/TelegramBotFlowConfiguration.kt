package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUALIFIER_FLOW_TG_BOT
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.PermissionsFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.*
import ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers.RegistrationActionHandlerFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers.RegistrationFetcher
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.belly.FlowContext

/**
 * Основной класс, в котором строится последовательность вычислений (граф) для бота
 */
@Configuration
open class TelegramBotFlowConfiguration(
    private val actualizeUserInfoFetcher: ActualizeUserInfoFetcher,
    private val weeklyEventsFetcher: WeeklyEventsFetcher,
    private val questStartFetcher: QuestStartFetcher,
    private val updateDataFetcher: UpdateDataFetcher,
    private val questButtonHandlerFetcher: QuestButtonHandlerFetcher,
    private val dialogHandleFetcher: DialogHandleFetcher,
    private val categoryButtonHandlerFetcher: CategoryButtonHandlerFetcher,
    private val categoryCommandHandlerFetcher: CategoryCommandHandlerFetcher,
    private val categoryActionTypeHandlerFetcher: CategoryActionTypeHandlerFetcher,
    private val registrationFetcher: RegistrationFetcher,
    private val userActionHandlerFetcher: RegistrationActionHandlerFetcher,
    private val roleDescriptionFetcher: RoleDescriptionFetcher,
    private val userInfoCommandFetcher: UserInfoCommandFetcher,
    private val settingMailFetcher: SettingMailFetcher,
    private val broadcastConstructorFetcher: BroadcastConstructorFetcher,
    private val permissionsFetcher: PermissionsFetcher,
) {
    /**
     * Возвращает построенный граф; выполняется только при запуске приложения
     */
    @Bean(QUALIFIER_FLOW_TG_BOT)
    open fun flowBuilder(): FlowBuilder {
        val flowBuilder = FlowBuilder()
        flowBuilder.buildFlow()
        return flowBuilder
    }

    private fun FlowBuilder.buildFlow() {
        sequence {
            fetch(actualizeUserInfoFetcher)

            // registration block
            sequence(condition = { it.isByUser() && !it.isUserRegistered() && it.isPersonalUpdate() }) {
                fetch(userActionHandlerFetcher)
                fetch(registrationFetcher)
            }

            group(condition = { it.isByUser() && it.isUserRegistered() }) {
                sequence {
                    fetch(categoryCommandHandlerFetcher)
                    fetch(categoryButtonHandlerFetcher)
                    fetch(categoryActionTypeHandlerFetcher)
                    fetch(settingMailFetcher)
                    fetch(questButtonHandlerFetcher)
                    fetch(dialogHandleFetcher)
                    fetch(broadcastConstructorFetcher)
                    fetch(permissionsFetcher)
                }

                fetch(roleDescriptionFetcher)
                fetch(userInfoCommandFetcher)
                fetch(weeklyEventsFetcher)
                fetch(questStartFetcher)
            }
            fetch(updateDataFetcher)
        }
    }

    private fun FlowContext.isByUser() = get<ExpContainer>()?.byUser ?: false

    private fun FlowContext.isUserRegistered() = get<UserActualizedInfo>()?.isRegistered ?: false

    private fun FlowContext.isPersonalUpdate() = get<ExpContainer>()?.isPersonal ?: false
}
