package ru.idfedorov09.telegram.bot.fetchers

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.GlobalConstants.hasCertificate
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class AddCertificateFetcher : DefaultFetcher() {

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