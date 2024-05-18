package ru.idfedorov09.telegram.bot.data.model

import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.mephi.sno.libs.flow.belly.Mutable

/**
 * Хранит актуализированную информацию о пользователе
 */
@Mutable
data class UserActualizedInfo(
    // null id - юзер не зарегистрирован
    val id: Long? = null,
    val tui: String,
    val lastTgNick: String? = null,
    val fullName: String? = null,
    val studyGroup: String? = null,
    val categories: MutableSet<Category> = mutableSetOf(),
    val roles: MutableSet<UserRole> = mutableSetOf(),
    var lastUserActionType: LastUserActionType? = null,
    /** Последний активный вопрос, заданный пользователем **/
    // TODO: readme -> активный вопрос - вопрос с диалогом
    val activeQuestDialog: QuestDialog?,
    /** Временные данные **/
    var data: UserData? = null,
    val isRegistered: Boolean,
    var isDeleted: Boolean = false,
    /** Текущая создаваемая рассылка пользователя **/
    var bcData: Broadcast? = null,
    /** Текущая информация о бане **/
    var isBaned: Boolean = false,
    /** Текущий создаваемый бан **/
    var banData: Ban? = null,
    /** Текущий создаваемый вопрос опроса**/
    var surveyQuestionData: SurveyQuestion? = null
)
