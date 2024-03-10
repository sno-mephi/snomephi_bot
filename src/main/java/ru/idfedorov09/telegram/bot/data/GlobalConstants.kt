package ru.idfedorov09.telegram.bot.data

import java.time.ZoneId

object GlobalConstants {

    const val HEALTH_PATH = "/is_alive"
    const val WEBHOOK_PATH = "/"

    const val QUALIFIER_FLOW_TG_BOT = "tg_bot_flow_builder"
    const val QUALIFIER_FLOW_HEALTH_STATUS = "health_flow_builder"
    const val QUEUE_PRE_PREFIX = "frjekcs_ewer_idfed09_user_bot_que_"

    const val MAX_MSG_LENGTH = 1024

    val BOT_TIME_ZONE = ZoneId.of("Europe/Moscow")

    // id админского чата
    // TODO: поменять на айдишники (возможность делать несколько админских чатов!)
    const val QUEST_RESPONDENT_CHAT_ID = "-1002057270905"

    /** беседа куда отправляется всякий треш**/
    const val TRASH_CHAT_ID = "-1002057270905"
}
