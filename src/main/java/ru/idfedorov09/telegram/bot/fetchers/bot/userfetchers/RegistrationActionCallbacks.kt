package ru.idfedorov09.telegram.bot.fetchers.bot.userfetchers

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands

fun userWithoutGroupActionCallback() = InlineKeyboardMarkup(
    listOf(
        listOf(
            InlineKeyboardButton("👾Я не из МИФИ").also {
                it.callbackData = CallbackCommands.USER_WITHOUT_GROUP.data
            }
        ),
    )
)

fun createActionsKeyboard(
    parameter: String
) = InlineKeyboardMarkup(
    listOf(
        listOf(
            InlineKeyboardButton("✅ Подтвердить").also {
                it.callbackData = CallbackCommands.USER_CONFIRM.data.format(parameter)
            },
            InlineKeyboardButton("❌ Отменить").also {
                it.callbackData = CallbackCommands.USER_DECLINE.data.format(parameter)
            }
        ),
    )
)