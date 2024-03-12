package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.User

object KeyBoardUtil {
    fun changeKeyBoard(userKeyboardType: UserKeyboardType, user: User): ReplyKeyboard {
        return when (userKeyboardType) {
            UserKeyboardType.WITHOUT_KEYBOARD -> deleteKeyBoard(user.roles)
            UserKeyboardType.DEFAULT_MAIN_BOT -> createDefaultKeyBoard(user.roles)
            UserKeyboardType.DIALOG_QUEST -> createDialogKeyBoard(user.roles)
        }
    }

    private fun deleteKeyBoard(roles: MutableSet<UserRole>): ReplyKeyboard {
        return ReplyKeyboardRemove().apply {
            removeKeyboard = true
        }
    }

    private fun createDefaultKeyBoard(roles: MutableSet<UserRole>): ReplyKeyboard {
        val keyboards = mutableListOf(
            KeyboardRow().also {
                it.add(TextCommands.SETTING_MAIL.commandText)
            },
            KeyboardRow().also {
                it.add(TextCommands.WEEKLY_EVENTS.commandText)
            },
        )
        if (UserRole.ROOT in roles || UserRole.MAILER in roles) {
            keyboards.add(
                KeyboardRow().also {
                    it.add(TextCommands.BROADCAST_CONSTRUCTOR.commandText)
                },
            )
        }
        val defaultKeyBoard = ReplyKeyboardMarkup().also {
            it.keyboard = keyboards
            it.oneTimeKeyboard = true
            it.resizeKeyboard = true
        }
        return defaultKeyBoard
    }

    private fun createDialogKeyBoard(roles: MutableSet<UserRole>): ReplyKeyboard {
        val keyboards = mutableListOf(
            KeyboardRow().also {
                it.add(TextCommands.SETTING_MAIL.commandText)
            },
            KeyboardRow().also {
                it.add(TextCommands.WEEKLY_EVENTS.commandText)
            },
        )
        val closeDialogKeyboard = ReplyKeyboardMarkup().also {
            it.keyboard = keyboards
            it.oneTimeKeyboard = true
            it.resizeKeyboard = true
        }
        return closeDialogKeyboard
    }
}
