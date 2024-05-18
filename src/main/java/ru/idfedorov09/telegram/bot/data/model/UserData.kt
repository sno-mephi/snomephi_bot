package ru.idfedorov09.telegram.bot.data.model

import ru.mephi.sno.libs.flow.belly.Mutable

/**
 Хранит временны данные
 **/
@Mutable
data class UserData(
    /** Уже не используется, но имет такой же смысл как и старая data **/
    var dataLegacy: String? = null,
    /** id сообщение консоли при разблокировки **/
    var unBanMessageId: Int? = null,
    /** id сообщение консоли при регистрации **/
    var registrationMessageId: Int? = null,
    /** поле для строковых временных данных при регистрации **/
    var registrationData: String? = null,
    /** id сообщение консоли при выдачи ролей **/
    var permissionMessageId: Int? = null,
    /** id сообщение консоли при настройке категорий **/
    var categoryMessageId: Int? = null,
    /** id сообщения настроек юзера **/
    var userSettingMessageId: Int? = null,
    /** id сообщения примера вопроса в опросе **/
    var surveyQuestionMessageId: Int? = null,
)
