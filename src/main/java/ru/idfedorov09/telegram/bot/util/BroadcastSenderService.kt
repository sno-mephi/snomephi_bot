package ru.idfedorov09.telegram.bot.util

import org.jvnet.hk2.annotations.Service
import org.springframework.scheduling.annotation.Scheduled

@Service
class BroadcastSenderService {
    @Scheduled(fixedDelay = 1000)
    fun broadcastSender(){

    }
}