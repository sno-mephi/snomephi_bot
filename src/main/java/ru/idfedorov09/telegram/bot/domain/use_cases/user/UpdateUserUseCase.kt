package ru.idfedorov09.telegram.bot.domain.use_cases.user

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Component
class UpdateUserUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(
        user: User
    ) = repository.save(user)
}