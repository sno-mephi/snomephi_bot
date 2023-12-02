package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.idfedorov09.telegram.bot.data.model.Ban

interface BanRepository : JpaRepository<Ban, Long> {
    @Query("SELECT a FROM Ban a WHERE a.userId = :user_id ")
    fun findByUserId(@Param("user_id")userId: Long?): Ban?
}
