package ru.idfedorov09.telegram.bot.data.model.extensions

import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.GlobalConstants.TRGM_SIMILARITY_THRESHOLD

@Component
class PgTrgmInitializer(private val jdbcTemplate: JdbcTemplate) {

    @PostConstruct
    fun init() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm")
        jdbcTemplate.execute("SET pg_trgm.similarity_threshold TO $TRGM_SIMILARITY_THRESHOLD")
    }
}