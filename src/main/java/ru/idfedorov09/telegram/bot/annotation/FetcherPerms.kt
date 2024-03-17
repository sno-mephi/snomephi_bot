package ru.idfedorov09.telegram.bot.annotation

import ru.idfedorov09.telegram.bot.data.enums.UserRole

/**
 * Требуется для определения ролей юзера, который может использовать фетчер
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FetcherPerms(vararg val roles: UserRole)
