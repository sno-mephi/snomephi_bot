package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
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
        nativeQuery = true,
    )
    fun findByTui(userTui: String): User?

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE User u 
            SET u.isKeyboardSwitched = :isSwitched 
            WHERE u.tui = :tui
        """,
    )
    fun updateKeyboardSwitchedForUserTui(
        tui: String,
        isSwitched: Boolean,
    )

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE User u 
            SET u.isKeyboardSwitched = :isSwitched 
            WHERE u.id = :userId
        """,
    )
    fun updateKeyboardSwitchedForUserId(
        userId: Long,
        isSwitched: Boolean,
    )

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Modifying
    @Query(
        """
            UPDATE User u 
            SET u.isKeyboardSwitched = false, u.currentKeyboardType = :newKeyboardType
            WHERE u.id = :userId
        """,
    )
    fun updateKeyboard(
        userId: Long,
        newKeyboardType: UserKeyboardType,
    )

    @Transactional
    @Modifying
    @Query(
        """
            UPDATE users_table
            SET categories = array_append(categories, :category_id)
        """,
        nativeQuery = true,
    )
    fun addCategoryForAllUser(
        @Param("category_id") categoryId: Long,
    )

    @Transactional
    @Modifying
    @Query(
        """
            UPDATE users_table
            SET categories = array_append(categories, :category_id)
            WHERE id = :user_id 
        """,
        nativeQuery = true,
    )
    fun addCategory(
        @Param("category_id") categoryId: Long,
        @Param("user_id") userId: Long,
    )

    @Transactional
    @Modifying
    @Query(
        """
            UPDATE users_table
            SET categories = array_remove(categories, :category_id)
            WHERE id = :user_id 
        """,
        nativeQuery = true,
    )
    fun removeCategory(
        @Param("category_id") categoryId: Long,
        @Param("user_id") userId: Long,
    )

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
        nativeQuery = true,
    )
    fun findByFullNameAndStudyGroup(
        fullName: String,
        studyGroup: String,
    ): User?

    @Query(
        """
            SELECT *
            FROM users_table
            WHERE 1 = 1
                and id = :userId
                and is_deleted = False
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findActiveUsersById(userId: Long?): User?

    @Query(
        """
            SELECT *
            FROM users_table
            WHERE 1 = 1
                and is_deleted = False
        """,
        nativeQuery = true,
    )
    fun findAllActiveUsers(): List<User?>
}
