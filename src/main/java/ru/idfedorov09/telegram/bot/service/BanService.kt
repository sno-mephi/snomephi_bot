package ru.idfedorov09.telegram.bot.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.idfedorov09.telegram.bot.repo.BanRepository

@Service
class BanService(
    private val banRepository: BanRepository,
) {

    /** функция раз в секунду проверяет времена конца бана*/
    @Scheduled(fixedDelay = 1000)
    fun banUpdate() {
        banRepository.updateBanInfo()
    }
}
