package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.idfedorov09.telegram.bot.data.model.Category

interface CategoryRepository : JpaRepository<Category, Long>{
    @Query(value = "SELECT u FROM category_table u LIMIT u.pageSize OFFSET u.page",
        nativeQuery = true)
    fun findCategoriesByPage(@Param("page")page: Long, @Param("pageSize")pageSize: Long): List<Category>
}
