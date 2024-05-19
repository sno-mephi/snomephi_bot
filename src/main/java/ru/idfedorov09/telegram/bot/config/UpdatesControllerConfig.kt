package ru.idfedorov09.telegram.bot.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.idfedorov09.telegram.bot.base.controller.UpdatesController
import ru.idfedorov09.telegram.bot.base.service.FlowBuilderService
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants.ROOT_LIST

@Configuration
open class UpdatesControllerConfig {

    @Autowired
    private lateinit var flowBuilderService: FlowBuilderService

    /**
     * Позволяем только рутам инициировать настройку (вызывать System Flow)
     */
    @Bean
    open fun updatesControllerBean(updatesUtil: UpdatesUtil): UpdatesController {
        val updatesController = UpdatesController(flowBuilderService)
        updatesController.setHasAccessToSystemFlow { params ->
            val user = updatesUtil.getUser(params.update) ?: return@setHasAccessToSystemFlow false
            return@setHasAccessToSystemFlow user.id.toString() in ROOT_LIST
        }
        return updatesController
    }
}