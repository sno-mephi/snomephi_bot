package ru.idfedorov09.telegram.bot.fetchers

import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * Фетчер, который выполняет также проверку на права, если требуется
 */
open class DefaultFetcher: GeneralFetcher() {

    override fun fetchCall(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
        params: MutableList<Any?>
    ): Any? {
        if (!isValidPerms(flowContext, doFetchMethod)) return null
        return super.fetchCall(flowContext, doFetchMethod, params)
    }

    private fun isValidPerms(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
    ): Boolean {
        val fetcherPermsAnnotation = doFetchMethod.findAnnotation<FetcherPerms>() ?: return true
        val user = flowContext.get<UserActualizedInfo>() ?: return true;
        if (UserRole.ROOT in user.roles) return true;
        val allowPerms = fetcherPermsAnnotation.roles
        return allowPerms.all { it in user.roles }
    }
}