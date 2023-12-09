package ru.idfedorov09.telegram.bot.util

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.model.Ban
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.repo.BanRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import java.time.LocalDateTime

@Component
class BanUtil(
    private val userRepository: UserRepository,
    private val banRepository: BanRepository,
) {
    fun banUser(user: User, comment: String, dateTime: LocalDateTime) {
        val ban: Ban = Ban(
            userId = user.id,
            comment = comment,
        )
        banRepository.save(ban)
    }

    fun unbanUser(user: User) {
        val ban = banRepository.findByUserId(user.id)!!
        banRepository.delete(ban)
    }
}
