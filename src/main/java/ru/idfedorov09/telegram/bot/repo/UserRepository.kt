package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import ru.idfedorov09.telegram.bot.data.model.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByTui(tui: String): User?

    fun findByFullNameAndStudyGroup(fullName: String, studyGroup: String): User?

    @Transactional
    @Modifying
    @Query(
        """
            UPDATE User u 
            SET u.isKeyboardSwitched = :isSwitched 
            WHERE u.tui = :tui
        """
    )
    fun updateKeyboardSwitchedForUser(tui: String, isSwitched: Boolean)
}
