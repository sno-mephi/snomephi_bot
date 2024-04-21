package ru.idfedorov09.telegram.bot.fetchers

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.util.OnReceiver
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * Фетчер, который выполняет также проверку на права, если требуется
 */
@Component
open class DefaultFetcher : GeneralFetcher() {
    protected lateinit var flowContext: FlowContext

    @Autowired
    private lateinit var messageSenderService: MessageSenderService

    companion object {
        private val log = LoggerFactory.getLogger(DefaultFetcher::class.java)
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
        }.onFailure { e ->
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

    /**
     * Метод который удаляет сообщение из только что пришедшего обновления
     * Вызывает исключение RuntimeException если в контексте нет Update или обновление не содержит сообщение
     */
    fun deleteUpdateMessage() {
        val update =
            flowContext.get<Update>()
                ?: throw RuntimeException("Can't delete the message: there's no update in the context.")

        if (!update.hasMessage()) {
            throw RuntimeException("Can't delete the message: there's no message in the update.")
        }

        messageSenderService.deleteMessage(
            MessageParams(
                chatId = update.message.chatId.toString(),
                messageId = update.message.messageId,
            ),
        )
    }
}
