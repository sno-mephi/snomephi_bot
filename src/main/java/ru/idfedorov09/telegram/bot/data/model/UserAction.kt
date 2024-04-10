package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.model.converter.LastUserActionTypeConverter
import java.time.LocalDateTime

@Entity
@Table(name = "user_actions_table")
data class UserAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_action_id")
    val id: Long? = null,
    @Column(name = "user_action_dttm")
    val actionTime: LocalDateTime? = null,
    @Column(name = "user_id")
    val userId: Long? = null,
    /** id юзера в телеграме **/
    @Column(name = "tui")
    val tui: String? = null,
    @Column(name = "last_action_type", columnDefinition = "TEXT")
    @Convert(converter = LastUserActionTypeConverter::class)
    val lastUserActionType: LastUserActionType? = null,
    @Column(name = "message_text", columnDefinition = "TEXT")
    val messageText: String? = null,
    @Column(name = "callback_data_id")
    val callbackDataId: Long? = null,
    @Column(name = "callback_data_legacy", columnDefinition = "TEXT")
    val callbackDataLegacy: String? = null,
)