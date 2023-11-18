package ru.idfedorov09.telegram.bot.domain.use_cases.user

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Component
class GetUserUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(tui: String): User? = repository.findByTui(tui)
}