package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.User

interface UserRepository : JpaRepository<User, Long> {

    @Query(
        """
            SELECT *
                FROM users_table
                WHERE 1 = 1
                and tui = :userTui
                and is_deleted = False
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findByTui(userTui: String): User?


    @Query(
        """
            SELECT *
                FROM users_table
                WHERE 1 = 1
                and full_name = :fullName
                and study_group = :studyGroup
                and is_deleted = False
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findByFullNameAndStudyGroup(fullName: String, studyGroup: String): User?


    @Query(
        """
            SELECT *
                FROM users_table
                WHERE 1 = 1
                and id = :userId
                and is_deleted = False
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findByUserId(userId: Long?): User?

}
