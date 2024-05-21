package ru.idfedorov09.telegram.bot.data

import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import java.time.ZoneId

object GlobalConstants {
    const val QUALIFIER_FLOW_TG_BOT = "tg_bot_flow_builder"
    const val QUALIFIER_FLOW_HEALTH_STATUS = "health_flow_builder"
    const val CONFIG_PREFIX = "config_prefix"
    const val MAX_CATEGORY_COUNTS = 25
    const val MAX_MSG_LENGTH = 1024

    val BOT_TIME_ZONE = ZoneId.of("Europe/Moscow")

    const val MAX_BROADCAST_BUTTONS_COUNT = 5

    val ROOT_LIST: List<String> = listOf(
        "920061911",
        "731119845",
        "473458128"
    )

    const val DOCTYPE_PDF = "application/pdf"
}
