package ru.idfedorov09.telegram.bot.util

import org.jvnet.hk2.annotations.Service
import org.springframework.scheduling.annotation.Scheduled
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Service
class BroadcastSenderService(
    private val broadcastRepository: BroadcastRepository,
    private val userRepository: UserRepository,
) {
    @Scheduled(fixedDelay = 1000)
    fun broadcastSender() {
        val firstActiveBroadcast = broadcastRepository.findFirstActiveBroadcast() ?: return
        userRepository.findAll().forEach{
            if (it.id !in firstActiveBroadcast.receivedUsersId) {

            }
            }
        }
    }
}
