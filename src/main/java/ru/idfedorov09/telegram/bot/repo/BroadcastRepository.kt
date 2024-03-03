package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.Broadcast

interface BroadcastRepository : JpaRepository<Broadcast, Long> {
    @Query("""
        SELECT b FROM Broadcast b
        WHERE b.authorId = :authorId AND b.isBuilt = false AND b.isDeleted = false
        ORDER BY b.id DESC
        LIMIT 1
    """)
    fun findLatestUnbuiltBroadcastByAuthor(authorId: Long): Broadcast?

}
