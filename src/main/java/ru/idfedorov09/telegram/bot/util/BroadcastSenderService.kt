package ru.idfedorov09.telegram.bot.util

import org.jvnet.hk2.annotations.Service
import org.springframework.scheduling.annotation.Scheduled
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository

@Service
class BroadcastSenderService(
    private val broadcastRepository: BroadcastRepository,
    private val userRepository: UserRepository,
    private val bot: Executor,
) {
    @Scheduled(fixedDelay = 1000)
    fun broadcastSender() {
        val firstActiveBroadcast = broadcastRepository.findFirstActiveBroadcast() ?: return
        val firstUser = userRepository.findAll().firstOrNull {
            it.id !in firstActiveBroadcast.receivedUsersId &&
                (
                    it.categories.intersect(firstActiveBroadcast.categoriesId).isEmpty() ||
                        firstActiveBroadcast.categoriesId.isEmpty()
                    )
        } ?: run {
            broadcastRepository.save(
                firstActiveBroadcast.copy(isCompleted = true),
            )
            return
        }
        if (firstActiveBroadcast.imageHash == null) {
            bot.execute(
                SendMessage().also {
                    it.chatId = firstUser.tui.toString()
                    it.text = firstActiveBroadcast.text.toString()
                },
            )
        } else {
            bot.execute(
                SendPhoto().also {
                    it.chatId = firstUser.tui.toString()
                    it.photo = InputFile(firstActiveBroadcast.imageHash)
                },
            )
        }
        firstUser.id?.let { firstActiveBroadcast.receivedUsersId.add(it) }
    }
}
