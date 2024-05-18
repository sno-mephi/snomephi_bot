package ru.idfedorov09.telegram.bot.flow

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.base.service.FlowBuilderService
import ru.idfedorov09.telegram.bot.data.GlobalConstants.QUALIFIER_FLOW_HEALTH_STATUS
import ru.idfedorov09.telegram.bot.fetchers.health.HealthStatusMapper
import ru.mephi.sno.libs.flow.belly.FlowBuilder

@Configuration
open class HealthStatusFlowConfiguration(
    private val healthStatusMapper: HealthStatusMapper,
    private val flowBuilderService: FlowBuilderService,
) {
    @Bean(QUALIFIER_FLOW_HEALTH_STATUS)
    open fun flowBuilder(): FlowBuilder {
        val flowBuilder = FlowBuilder()
        flowBuilder.buildFlow()
        flowBuilderService.register(QUALIFIER_FLOW_HEALTH_STATUS, flowBuilder)
        return flowBuilder
    }

    private fun FlowBuilder.buildFlow() {
        fetch(healthStatusMapper)
    }
}
