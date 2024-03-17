package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage

interface QuestDialogMessageRepository : JpaRepository<QuestDialogMessage, Long>
