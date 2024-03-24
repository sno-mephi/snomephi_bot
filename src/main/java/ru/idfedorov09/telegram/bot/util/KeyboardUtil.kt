package ru.idfedorov09.telegram.bot.util

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.User

object KeyboardUtil {
    fun changeKeyboard(
        userKeyboardType: UserKeyboardType,
        user: User,
    ): ReplyKeyboard {
        return when (userKeyboardType) {
            UserKeyboardType.WITHOUT_KEYBOARD -> deleteKeyboard(user.roles)
            UserKeyboardType.DEFAULT_MAIN_BOT -> createDefaultKeyboard(user.roles)
            UserKeyboardType.DIALOG_QUEST -> createDialogKeyboard(user.roles)
        }
    }

    private fun deleteKeyboard(roles: MutableSet<UserRole>) = ReplyKeyboardRemove().apply { removeKeyboard = true }

    private fun createDefaultKeyboard(roles: MutableSet<UserRole>): ReplyKeyboard {
        val keyboards =
            mutableListOf(
                KeyboardRow().also {
                    it.add(TextCommands.SETTING_MAIL())
                },
                KeyboardRow().also {
                    it.add(TextCommands.WEEKLY_EVENTS())
                },
            )
        if (UserRole.ROOT in roles || UserRole.MAILER in roles) {
            keyboards.add(
                KeyboardRow().also {
                    it.add(TextCommands.BROADCAST_CONSTRUCTOR())
                },
            )
        }
        if (UserRole.ROOT in roles || UserRole.CATEGORY_BUILDER in roles) {
            keyboards.add(
                KeyboardRow().also {
                    it.add(TextCommands.CATEGORY_CHOOSE_TEXT_ACTION())
                }
            )
        }
        val defaultKeyboard =
            ReplyKeyboardMarkup().also {
                it.keyboard = keyboards
                it.resizeKeyboard = true
            }
        return defaultKeyboard
    }

    private fun createDialogKeyboard(roles: MutableSet<UserRole>): ReplyKeyboard {
        val keyboards =
            mutableListOf(
                KeyboardRow().also {
                    it.add(TextCommands.QUEST_DIALOG_CLOSE())
                },
            )
        val closeDialogKeyboard =
            ReplyKeyboardMarkup().also {
                it.keyboard = keyboards
                it.resizeKeyboard = true
            }
        return closeDialogKeyboard
    }
}
