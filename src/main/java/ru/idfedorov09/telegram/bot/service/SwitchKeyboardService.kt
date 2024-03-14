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
    // forceSwitch - иногда (например, при выдаче роли) требуется принудительно обновить клавиатуру
    fun switchKeyboard(
        userId: Long,
        newKeyboardType: UserKeyboardType,
        forceSwitch: Boolean = false,
    ) {
        val user = userRepository.findById(userId).getOrNull() ?: return
        user.apply {
            if (currentKeyboardType == newKeyboardType && !forceSwitch) return
            userRepository.updateKeyboard(userId, newKeyboardType)
        }
    }

    fun disableKeyboard(
        userId: Long,
        forceSwitch: Boolean = false,
    ) = switchKeyboard(userId, UserKeyboardType.WITHOUT_KEYBOARD, forceSwitch)

    fun reshowKeyboard(userId: Long) = userRepository.updateKeyboardSwitchedForUserId(userId, false)
}
