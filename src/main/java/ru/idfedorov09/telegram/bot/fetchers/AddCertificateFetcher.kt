package ru.idfedorov09.telegram.bot.fetchers

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.GlobalConstants.DOCTYPE_PDF
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.Certificate
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.repo.CertificateRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class AddCertificateFetcher(
    private val userRepository: UserRepository,
    private val certificateRepository: CertificateRepository,
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
        // TODO: дописать в случае если не нашелся челик - поиск по косинусной близости + сообщение об этом админу
    }

    /**
     * Возвращает ФИО в нижнем регистре
     * ВАЖНО: сравнение по ФИО тоже проводить в нижнем регистре
     */
    private fun getFullName(update: Update) =
        update.message.document.fileName
            .lowercase()
            .removeSuffix(".pdf")
            .replace("_", " ")

    private fun Certificate.save() = certificateRepository.save(this)

    private data class Params(
        val update: Update,
        val userActualizedInfo: UserActualizedInfo,
        var certificate: Certificate,
    )
}