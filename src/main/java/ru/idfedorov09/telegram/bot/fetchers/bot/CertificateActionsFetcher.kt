package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.CertificateRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class CertificateActionsFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val certificateRepository: CertificateRepository,
    private val messageSenderService: MessageSenderService,
) : DefaultFetcher() {

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        if (update.hasMessage() && update.message.hasText() && update.message.text == TextCommands.GET_CERTIFICATE())
            getCertificate(update, userActualizedInfo)
        else if (update.hasCallbackQuery()) callbackQueryHandler(update, userActualizedInfo)
    }

    private fun callbackQueryHandler(update: Update, userActualizedInfo: UserActualizedInfo) {
        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.CANCEL_CONFIRM_FULLNAME.data) ->
                    cancelConfirmFullName(update, userActualizedInfo)
                startsWith(CallbackCommands.APPROVE_CONFIRM_FULLNAME.data) ->
                    approveConfirmFullName(update, userActualizedInfo)
            }
        }
    }

    private fun getCertificate(update: Update, userActualizedInfo: UserActualizedInfo) {
        val certificate = certificateRepository.findByCertificateOwnerId(userActualizedInfo.id!!) ?: run {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = "Ваш сертификат пока не готов. Как только он будет готов, " +
                            "Вы сможете его получить при повторном нажатии " +
                            "на кнопку '${TextCommands.GET_CERTIFICATE.commandText}'."
                )
            )
            return
        }

        messageSenderService.sendMessage(
            MessageParams(
                chatId = userActualizedInfo.tui,
                document = InputFile(certificate.certificateHash)
            )
        )
    }

    private fun cancelConfirmFullName(update: Update, userActualizedInfo: UserActualizedInfo) {
        // TODO
    }

    private fun approveConfirmFullName(update: Update, userActualizedInfo: UserActualizedInfo) {
        // TODO
    }
}