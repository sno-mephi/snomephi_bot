package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service
import ru.idfedorov09.telegram.bot.repo.CategoryRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository

/**
 Сервис для изменения категорий юзеров
 */
@Service
class CategoryUpdateService(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
) {
    fun toggleCategory(
        userId: Long?,
        categoryId: Long,
    ) {
        categoryRepository.findById(categoryId) ?: return
        val user = userId?.let { userRepository.findById(it).get() } ?: return
        if (user.categories.contains(categoryId)) {
            userRepository.removeCategory(
                categoryId = categoryId,
                userId = userId,
            )
        } else {
            (
                userRepository.addCategory(
                    categoryId = categoryId,
                    userId = userId,
                )
            )
        }
    }
}
