package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.idfedorov09.telegram.bot.data.model.Category

interface CategoryRepository : JpaRepository<Category, Long> {
    @Query(
        value = "SELECT * FROM category_table " +
            "WHERE changed_by_tui IS NULL " +
            "ORDER BY suffix ASC " +
            "LIMIT :pageSize OFFSET :page*6",
        nativeQuery = true,
    )
    fun findCategoriesByPage(@Param("page") page: Long, @Param("pageSize") pageSize: Long): List<Category>

    fun findByChangedByTui(changingByTui: String): Category?

    fun findBySuffix(suffix: String): Category?
}
