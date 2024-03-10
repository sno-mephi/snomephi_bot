package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Service
class KeyBoardService(
    private val bot: Executor,
    private val userRepository: UserRepository,
) {
    fun changeKeyBoard(
        userId: Long,
        userKeyboardType: UserKeyboardType,
    ) {
        val user = userRepository.findById(userId).get()
        if (user.currentKeyboardType == userKeyboardType) return
        when (userKeyboardType) {
            UserKeyboardType.WITHOUT_KEYBOARD -> deleteKeyBoard(user, userKeyboardType)
            UserKeyboardType.DEFAULT_MAIN_BOT -> createDefaultKeyBoard(user, userKeyboardType)
            UserKeyboardType.DIALOG_QUEST -> createDialogKeyBoard(user, userKeyboardType)
        }

        userRepository.save(
            user.copy(
                currentKeyboardType = userKeyboardType,
            ),
        )
    }

    private fun createDefaultKeyBoard(user: User, userKeyboardType: UserKeyboardType) {}
    private fun createDialogKeyBoard(user: User, userKeyboardType: UserKeyboardType) {
        val closeDialogKeyboard = ReplyKeyboardMarkup().also {
            it.keyboard = listOf(
                KeyboardRow().also {
                    it.add(TextCommands.QUEST_DIALOG_CLOSE.commandText)
                },
            )
            it.oneTimeKeyboard = true
            it.resizeKeyboard = true
        }
        bot.execute(
            SendMessage().also {
                it.chatId = user.tui.toString()
                it.replyMarkup = closeDialogKeyboard
            },
        )
    }

    private fun deleteKeyBoard(user: User, userKeyboardType: UserKeyboardType) {
        bot.execute(
            SendMessage().also {
                it.chatId = user.tui.toString()
                it.replyMarkup = ReplyKeyboardRemove().apply {
                    removeKeyboard = true
                }
            },
        )
    }
}
