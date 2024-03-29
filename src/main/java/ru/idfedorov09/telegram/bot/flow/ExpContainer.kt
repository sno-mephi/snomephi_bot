package ru.idfedorov09.telegram.bot.flow

import ru.idfedorov09.telegram.bot.data.enums.BotStage
import ru.mephi.sno.libs.flow.belly.Mutable

/**
 * Объект контекста флоу, содержащий информацию о работающих фичах, режимах и тд и тп
 */
@Mutable
data class ExpContainer(
    var botStage: BotStage = BotStage.OFFLINE,
    /** флаг, который говорит, что апдейт пришел от пользователя **/
    var byUser: Boolean = true,
    var isPersonal: Boolean = false,
    /** флаг принудительной остановки флоу. Если false то флоу принудительно останавливается **/
    var shouldContinueExecutionFlow: Boolean = true,
)
