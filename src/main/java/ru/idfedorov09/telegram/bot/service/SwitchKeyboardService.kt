package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.repo.UserRepository
import kotlin.jvm.optionals.getOrNull

/**
 * Сервис для изменения reply-клавиатуры у пользователя
 */
@Service
class SwitchKeyboardService(
    private val userRepository: UserRepository,
) {

    fun switchKeyboard(
        userId: Long,
        newKeyboardType: UserKeyboardType,
    ) {
        val user = userRepository.findById(userId).getOrNull() ?: return
        user.apply {
            if (currentKeyboardType == newKeyboardType) return
            userRepository.updateKeyboard(userId, newKeyboardType)
        }
    }

    fun disableKeyboard(userId: Long) = switchKeyboard(userId, UserKeyboardType.WITHOUT_KEYBOARD)
}