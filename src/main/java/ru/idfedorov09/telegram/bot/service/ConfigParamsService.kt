package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import ru.idfedorov09.telegram.bot.base.service.RedisService
import ru.idfedorov09.telegram.bot.data.enums.ConfigParamType
import ru.idfedorov09.telegram.bot.data.enums.ConfigParams

@Service
class ConfigParamsService(
    private val redisService: RedisService,
) {

    companion object {
        const val REDIS_PREFIX = "snomephi_bot_config_param_"
    }

    private operator fun ConfigParams.invoke() = "$REDIS_PREFIX#${this.key}"

    fun getValue(configParams: ConfigParams) =
        redisService.getSafe(configParams()) ?: configParams.defaultValues.getOrNull(0)

    /**
     * Устанавливает значение для параметра типа INPUT
     */
    fun setValue(configParams: ConfigParams, value: String) {
        if (configParams.type != ConfigParamType.INPUT)
            throw IllegalStateException("Неверный тип параметра.")

        redisService.setValue(configParams(), value)
    }
}