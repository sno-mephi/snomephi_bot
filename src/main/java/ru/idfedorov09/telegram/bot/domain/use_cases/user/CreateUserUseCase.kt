package ru.idfedorov09.telegram.bot.domain.use_cases.user

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Component
class CreateUserUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(
        fullName: String? = null,
        tui: String? = null,
        lastTgNick: String? = null,
        studyGroup: String? = null
    ) = repository.save(
        User(
            fullName = fullName,
            tui = tui,
            lastTgNick = lastTgNick,
            studyGroup = studyGroup
        )
    )
}