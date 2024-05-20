package ru.idfedorov09.telegram.bot.fetchers

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants.BOT_TIME_ZONE
import ru.idfedorov09.telegram.bot.data.GlobalConstants.DOCTYPE_PDF
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.Certificate
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.CertificateRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.LocalDateTime

@Component
class AddCertificateFetcher(
    private val userRepository: UserRepository,
    private val certificateRepository: CertificateRepository,
    private val updatesUtil: UpdatesUtil,
    private val messageSenderService: MessageSenderService,
    private val callbackDataRepository: CallbackDataRepository,
) : DefaultFetcher() {

    companion object {
        // Проверка на сертификат: считаем, что отправили сертификат, если написал рут или мэилер и тип файла ПДФ
        val hasCertificate: Update.(MutableSet<UserRole>) -> Boolean = { roles ->
            (roles.contains(UserRole.MAILER) || roles.contains(UserRole.ROOT)) &&
                    this.hasMessage() && this.message.hasDocument() && this.message.document.mimeType == DOCTYPE_PDF
        }
    }

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        // базовая фильтрация - проходят только документы с типом PDF + если юзер ничо такого не делает
        userActualizedInfo.apply {
            if (lastUserActionType != LastUserActionType.DEFAULT) return
            if (!update.hasCertificate(roles)) return
        }
        val fullName = getFullName(update)

        // TODO: если сертификат на такое ФИО уже есть, то скипаем все остальное

        val certificate = Certificate(
            fullName = fullName,
            issueAuthorId = userActualizedInfo.id,
            certificateHash = update.message.document.fileId,
        ).save()

        val params = Params(
            update = update,
            userActualizedInfo = userActualizedInfo,
            certificate = certificate,
        )

        val user = userRepository.findByLowercaseFullName(fullName) ?: run {
            onNotFoundByFullName(params)
            return
        }

        certificate.copy(certificateOwnerId = user.id).save()
    }

    private fun onNotFoundByFullName(params: Params) {
        params.apply {
            val similarUser = userRepository.findSimilarUserByFullName(certificate.fullName!!, 0.75) ?: run {
                onNotFoundByFullNameEvenSimilar(params)
                return
            }

            val okButton = CallbackData(callbackData = CallbackCommands.CANCEL_CONFIRM_FULLNAME.data, metaText = "\uD83D\uDD34").save()
            val cancelButton = CallbackData(callbackData = CallbackCommands.APPROVE_CONFIRM_FULLNAME.data, metaText = "\uD83D\uDFE2").save()

            val keyboard = listOf(
                listOf(cancelButton, okButton).map { button ->
                    InlineKeyboardButton().also {
                        it.text = button.metaText!!
                        it.url = button.metaUrl!!
                        it.callbackData = button.callbackData
                    }
                }
            )

            val sent = messageSenderService.sendMessage(
                MessageParams(
                    chatId = similarUser.tui!!,
                    text = "Здравствуйте!\n\nПри подготовке сертификата на ФИО ${certificate.fullName} " +
                            "бот не обнаружил пользователей с таким ФИО.\nНаша система обнаружила, " +
                            "что ваше ФИО похоже на указанное.\n\n<i>Если это ваше ФИО, то нажмите на зеленую кнопку, " +
                            "если нет - то нажмите на красную или проигнорируйте это сообщение.</i>",
                    parseMode = ParseMode.HTML,
                    replyMarkup = createKeyboard(keyboard),
                )
            )

            // отмечаем что начали опрос
            certificate.copy(
                pollStartTime = LocalDateTime.now().atZone(BOT_TIME_ZONE).toLocalDateTime(),
                pollMessageId = sent.messageId
            ).save()
        }
    }

    private fun onNotFoundByFullNameEvenSimilar(params: Params) {
        params.apply {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = updatesUtil.getChatId(update)!!,
                    text = "<b><i> ❗\uFE0F Я не нашел в базе человека с ФИО, " +
                            "даже похожей на ${certificate.fullName}.</i></b>",
                    parseMode = ParseMode.HTML
                )
            )
            certificateRepository.delete(certificate)
        }
    }

    private fun getFullName(update: Update): String {
        val input = update.message.document.fileName
        val parts = input.split(".")
        val textWithoutExtension = if (parts.size > 1) parts.dropLast(1).joinToString(".") else input
        return textWithoutExtension.replace("_", " ")
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun Certificate.save() = certificateRepository.save(this)
    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        var certificate: Certificate,
    )
}