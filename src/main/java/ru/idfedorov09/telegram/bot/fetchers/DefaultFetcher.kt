package ru.idfedorov09.telegram.bot.fetchers

import org.slf4j.LoggerFactory
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.util.OnReceiver
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * Фетчер, который выполняет также проверку на права, если требуется
 */
open class DefaultFetcher : GeneralFetcher() {

    private lateinit var flowContext: FlowContext

    companion object {
        private val log = LoggerFactory.getLogger(OnReceiver::class.java)
    }

    override fun fetchCall(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
        params: MutableList<Any?>,
    ): Any? {
        this.flowContext = flowContext
        val exp = this.flowContext.get<ExpContainer>() ?: ExpContainer()
        if (!exp.shouldContinueExecutionFlow) {
            return null
        }
        if (!isValidPerms(flowContext, doFetchMethod)) return null

        return runCatching {
            super.fetchCall(flowContext, doFetchMethod, params)
        }.onFailure {  e ->
            log.error("ERROR: $e")
            log.debug(e.stackTraceToString())
            stopFlowNextExecution()
        }.getOrNull()
    }

    private fun isValidPerms(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
    ): Boolean {
        val fetcherPermsAnnotation = doFetchMethod.findAnnotation<FetcherPerms>() ?: return true
        val user = flowContext.get<UserActualizedInfo>() ?: return true
        if (UserRole.ROOT in user.roles) return true
        val allowPerms = fetcherPermsAnnotation.roles
        return allowPerms.all { it in user.roles }
    }

    /**
     * Прерывает дальнейшее выполнение графа в рамках сессии (прогонки графа)
     */
    @Synchronized
    fun stopFlowNextExecution() {
        val exp = flowContext.get<ExpContainer>() ?: ExpContainer()
        exp.apply {
            shouldContinueExecutionFlow = false
            flowContext.insertObject(this)
        }
    }
}
