package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUALIFIER_FLOW_TG_BOT
import ru.idfedorov09.telegram.bot.fetchers.bot.* // ktlint-disable no-wildcard-imports
import ru.idfedorov09.telegram.bot.fetchers.bot.user_fetchers.RegistrationFetcher
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
    private val registrationFetcher: RegistrationFetcher,

    // TODO: вписать в граф
    private val roleDescriptionFetcher: RoleDescriptionFetcher,
    private val userInfoCommandFetcher: UserInfoCommandFetcher,
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
            group(condition = { it.isByUser() }) {
                fetch(registrationFetcher)
                fetch(roleDescriptionFetcher)
                fetch(userInfoCommandFetcher)
                fetch(questStartFetcher)
                fetch(questButtonHandlerFetcher)
                fetch(dialogHandleFetcher)
            }
            fetch(updateDataFetcher)
        }
    }

    private fun FlowContext.isByUser() = get<ExpContainer>()?.byUser ?: false
}
