package ru.idfedorov09.telegram.bot.data

import java.time.ZoneId

object GlobalConstants {

    const val MAX_CATEGORY_COUNTS = 25
    const val MAX_MSG_LENGTH = 1024

    val BOT_TIME_ZONE = ZoneId.of("Europe/Moscow")

    const val MAX_BROADCAST_BUTTONS_COUNT = 5

    val ROOT_LIST: List<String> = listOf(
        "920061911",
        "731119845",
        "473458128"
    )
}
