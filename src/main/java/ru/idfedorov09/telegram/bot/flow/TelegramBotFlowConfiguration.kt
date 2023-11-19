package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUALIFIER_FLOW_TG_BOT
import ru.idfedorov09.telegram.bot.fetchers.bot.*
import ru.mephi.sno.libs.flow.belly.FlowBuilder

/**
 * Основной класс, в котором строится последовательность вычислений (граф) для бота
 */
@Configuration
open class TelegramBotFlowConfiguration(
    // TODO: удалить лишние фетчеры
    private val testFetcher: TestFetcher,
    private val toggleStageFetcher: ToggleStageFetcher,

    private val actualizeUserInfoFetcher: ActualizeUserInfoFetcher,
    private val questStartFetcher: QuestStartFetcher,
    private val updateDataFetcher: UpdateDataFetcher,
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
            group(condition = { it.get<ExpContainer>()!!.byUser }) {
                fetch(questStartFetcher)
            }
            fetch(updateDataFetcher)
        }
    }
}
