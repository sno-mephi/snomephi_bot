package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.fetchers.health.HealthStatusMapper
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.config.BaseFlowConfiguration

@Configuration
open class HealthStatusFlowConfiguration(
    private val healthStatusMapper: HealthStatusMapper,
): BaseFlowConfiguration(HealthStatusFlowConfiguration::class) {

    override fun FlowBuilder.buildFlow() {
        fetch(healthStatusMapper)
    }
}
