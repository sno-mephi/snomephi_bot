package ru.idfedorov09.telegram.bot.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
open class BroadcastSenderService(
    private val broadcastRepository: BroadcastRepository,
    private val userRepository: UserRepository,
    private val buttonRepository: ButtonRepository,
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BroadcastSenderService::class.java)
    }


    // TODO: остается проблема - а если бродкастов несколько?
    @Scheduled(fixedDelay = 150)
    fun broadcastSender() {
        runCatching {
            trySendBroadcast()
        }.onFailure { e ->
            log.warn("Ошибка при отправке рассылки broadcastSender(): $e")
            log.debug(e.stackTraceToString())
        }
    }

    private fun trySendBroadcast() {
        val firstActiveBroadcast = broadcastRepository.findFirstActiveBroadcast() ?: return
        if (firstActiveBroadcast.receivedUsersId.isEmpty()) startBroadcast(firstActiveBroadcast)
        val firstUser =
            userRepository.findAllActiveUsers().filter { it!!.isRegistered }.firstOrNull {
                checkValidUser(it!!, firstActiveBroadcast)
            } ?: run {
                finishBroadcast(firstActiveBroadcast)
                return
            }
        runCatching {
            sendBroadcast(firstUser, firstActiveBroadcast)
        }.onFailure { e ->
            log.warn("Ошибка при отправке рассылки trySendBroadcast(): $e")
            log.debug("Send to user={}, broadcast={}", firstUser, firstActiveBroadcast)
            log.debug(e.stackTraceToString())

            if (e.message?.contains("429") != true) {
                addUserToFailedList(
                    userId = firstUser.id!!,
                    broadcast = firstActiveBroadcast,
                )
            }
        }
    }

    fun sendBroadcast(
        userId: Long,
        broadcast: Broadcast,
        shouldAddToReceived: Boolean = true,
    ) {
        val user = userRepository.findActiveUsersById(userId) ?: return
        sendBroadcast(user, broadcast, shouldAddToReceived)
    }

    private fun sendBroadcast(
        user: User,
        broadcast: Broadcast,
        shouldAddToReceived: Boolean = true,
    ) {
        messageSenderService.sendMessage(
            MessageParams(
                chatId = user.tui!!,
                text = broadcast.text,
                replyMarkup = createChooseKeyboard(broadcast),
                parseMode = ParseMode.HTML,
                photo = broadcast.imageHash?.let { InputFile(it) },
            ),
        )

        if (shouldAddToReceived) {
            addUserToReceivedList(
                userId = user.id!!,
                broadcast = broadcast,
            )
        }
    }

    private fun addUserToReceivedList(
        userId: Long,
        broadcast: Broadcast,
    ) {
        broadcast.receivedUsersId.add(userId)
        broadcastRepository.save(broadcast)
    }

    private fun addUserToFailedList(
        userId: Long,
        broadcast: Broadcast,
    ) {
        broadcast.failedUsersId.add(userId)
        broadcastRepository.save(broadcast)
    }

    private fun startBroadcast(broadcast: Broadcast) {
        val author = broadcast.authorId?.let { userRepository.findActiveUsersById(it) } ?: return
        val msgText = "Рассылка №${broadcast.id} успешно запущена"

        runCatching {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = author.tui!!,
                    text = msgText,
                ),
            )
        }
    }

    fun finishBroadcast(broadcast: Broadcast) {
        val finalBroadcast =
            broadcast.copy(
                isCompleted = true,
                finishTime =
                    LocalDateTime.now(
                        ZoneId.of("Europe/Moscow"),
                    ),
            )

        broadcastRepository.save(finalBroadcast)
        val author = finalBroadcast.authorId?.let { userRepository.findActiveUsersById(it)} ?: return

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

        val msgText =
                "Рассылка №${finalBroadcast.id} успешно завершена\n" +
                "Число пользователей, получивших сообщение: ${finalBroadcast.receivedUsersId.size}\n" +
                "Число пользователей, не получивших сообщение: ${finalBroadcast.failedUsersId.size}\n" +
                "Старт рассылки: ${finalBroadcast.startTime?.format(formatter)}\n" +
                "Конец рассылки: ${finalBroadcast.finishTime?.format(formatter)}"

        runCatching {
            messageSenderService.sendMessage(
                MessageParams(
                    chatId = author.tui!!,
                    text = msgText,
                ),
            )
        }
    }

    private fun checkValidUser(
        user: User,
        broadcast: Broadcast,
    ): Boolean {
        return user.id !in broadcast.receivedUsersId && (
            user.categories.intersect(broadcast.categoriesId).isNotEmpty() ||
                broadcast.categoriesId.isEmpty()
        ) && user.id !in broadcast.failedUsersId
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(firstActiveBroadcast: Broadcast): InlineKeyboardMarkup {
        val keyboardList =
            buttonRepository
                .findAllValidButtonsForBroadcast(firstActiveBroadcast.id!!)
                .map {
                    CallbackData(
                        callbackData = it.callbackData,
                        metaText = it.text,
                        metaUrl = it.link,
                    ).save()
                }

        val keyboard =
            keyboardList.map { callbackData ->
                listOf(
                    InlineKeyboardButton().also {
                        it.text = callbackData.metaText!!
                        it.callbackData = callbackData.id?.toString()
                        it.url = callbackData.metaUrl
                    },
                )
            }

        return createKeyboard(keyboard)
    }

    private fun CallbackData.save() = callbackDataRepository.save(this)
}
