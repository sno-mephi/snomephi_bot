package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUALIFIER_FLOW_TG_BOT
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.bot.ActualizeUserInfoFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.BroadcastConstructorFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.CategoryActionTypeHandlerFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.CategoryButtonHandlerFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.CategoryCommandHandlerFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.DialogHandleFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.QuestButtonHandlerFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.QuestStartFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.RoleDescriptionFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.UpdateDataFetcher
import ru.idfedorov09.telegram.bot.fetchers.bot.UserInfoCommandFetcher
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
    private val broadcastConstructorFetcher: BroadcastConstructorFetcher,
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
            group(condition = { it.isByUser() && !it.isUserRegistered() }) {
                fetch(userActionHandlerFetcher)
                fetch(registrationFetcher)
            }

            group(condition = { it.isByUser() && it.isUserRegistered() }) {
                fetch(categoryCommandHandlerFetcher)
                fetch(categoryButtonHandlerFetcher)
                fetch(categoryActionTypeHandlerFetcher)

                fetch(roleDescriptionFetcher)
                fetch(userInfoCommandFetcher)

                fetch(questStartFetcher)
                fetch(questButtonHandlerFetcher)
                fetch(dialogHandleFetcher)
                fetch(broadcastConstructorFetcher)
            }
            fetch(updateDataFetcher)
        }
    }

    private fun FlowContext.isByUser() = get<ExpContainer>()?.byUser ?: false

    private fun FlowContext.isUserRegistered() = get<UserActualizedInfo>()?.isRegistered ?: false
}
