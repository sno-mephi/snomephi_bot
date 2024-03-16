package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.idfedorov09.telegram.bot.data.enums.UserKeyboardType
import ru.idfedorov09.telegram.bot.data.model.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByTui(tui: String): User?

    fun findByFullNameAndStudyGroup(
        fullName: String,
        studyGroup: String,
    ): User?

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
}
