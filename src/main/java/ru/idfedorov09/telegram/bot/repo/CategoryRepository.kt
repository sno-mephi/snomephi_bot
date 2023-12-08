package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.idfedorov09.telegram.bot.data.model.Category

interface CategoryRepository : JpaRepository<Category, Long> {
    @Query(
        value = "SELECT * FROM category_table WHERE changing_by_tui IS NULL LIMIT :pageSize OFFSET :page*6",
        nativeQuery = true,
    )
    fun findCategoriesByPage(@Param("page") page: Long, @Param("pageSize") pageSize: Long): List<Category>

    fun findByChangingByTui(changingByTui: String): Category?

    fun findAllBySuffix(suffix: String): List<Category>
}
