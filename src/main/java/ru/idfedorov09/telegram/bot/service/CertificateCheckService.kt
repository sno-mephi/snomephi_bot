package ru.idfedorov09.telegram.bot.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.model.Certificate
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.repo.CertificateRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import kotlin.jvm.optionals.getOrNull

@Service
class CertificateCheckService(
    private val certificateRepository: CertificateRepository,
    private val messageSenderService: MessageSenderService,
    private val userRepository: UserRepository,
) {

    @Scheduled(fixedDelay = 5 * 1000)
    fun checkCertificates() {
        val certificate = certificateRepository.findFirstExpiredCertificate() ?: return
        onNotFindCertificateOwner(certificate)
    }

    fun onNotFindCertificateOwner(certificate: Certificate) {
        messageSenderService.sendMessage(
            MessageParams(
                chatId = GlobalConstants.QUEST_RESPONDENT_CHAT_ID,
                text = "\uD83D\uDE2D\uD83D\uDE2D\uD83D\uDE2D Я не смог найти человека " +
                        "с ФИО <code>${certificate.fullName}</code> для отправки сертификата",
                parseMode = ParseMode.HTML,
            )
        )

        val badOwner = certificate.candidateOwnerId?.let { userRepository.findById(it).getOrNull() }

        certificate.pollMessageId?.let { msgId ->
            badOwner?.tui?.let { ownerTui ->
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = ownerTui,
                        messageId = msgId
                    )
                )
            }
        }
        certificateRepository.delete(certificate)
    }
}