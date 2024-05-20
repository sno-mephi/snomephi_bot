package ru.idfedorov09.telegram.bot.fetchers

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.GlobalConstants.DOCTYPE_PDF
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class AddCertificateFetcher : DefaultFetcher() {

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
        // базовая фильтрация - проходят только документы с типом PDF
        userActualizedInfo.apply {
            if (lastUserActionType != LastUserActionType.DEFAULT) return
            if (!update.hasCertificate(roles)) return
        }
    }

}