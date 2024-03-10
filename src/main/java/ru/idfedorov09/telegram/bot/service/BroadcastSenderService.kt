package ru.idfedorov09.telegram.bot.service
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull

@Service
class BroadcastSenderService(
    private val broadcastRepository: BroadcastRepository,
    private val userRepository: UserRepository,
    private val buttonRepository: ButtonRepository,
    private val bot: Executor,
    private val callbackDataRepository: CallbackDataRepository
) {
    @Scheduled(fixedDelay = 1000)
    fun broadcastSender() {
        val firstActiveBroadcast = broadcastRepository.findFirstActiveBroadcast() ?: return
        if (firstActiveBroadcast.receivedUsersId.isEmpty()) startBroadcast(firstActiveBroadcast)
        val firstUser = userRepository.findAll().firstOrNull { checkValidUser(it, firstActiveBroadcast) } ?: run {
            finishBroadcast(firstActiveBroadcast)
            return
        }
        sendBroadcast(firstUser, firstActiveBroadcast)
    }

    fun sendBroadcast(userId: Long, broadcast: Broadcast, shouldAddToReceived: Boolean = true) {
        val user = userRepository.findById(userId).getOrNull() ?: return
        sendBroadcast(user, broadcast, shouldAddToReceived)
    }

    private fun sendBroadcast(user: User, broadcast: Broadcast, shouldAddToReceived: Boolean = true) {
        if (broadcast.imageHash == null) {
            bot.execute(
                SendMessage().also {
                    it.chatId = user.tui.toString()
                    it.text = broadcast.text.toString()
                    it.replyMarkup = createChooseKeyboard(broadcast)
                    it.parseMode = ParseMode.HTML
                },
            )
        } else {
            bot.execute(
                SendPhoto().also {
                    it.chatId = user.tui.toString()
                    it.photo = InputFile(broadcast.imageHash)
                    it.replyMarkup = createChooseKeyboard(broadcast)
                    it.parseMode = ParseMode.HTML
                },
            )
        }
        if (shouldAddToReceived) {
            user.id?.let {
                broadcast.receivedUsersId.add(it)
                broadcastRepository.save(broadcast)
            }
        }
    }

    private fun startBroadcast(broadcast: Broadcast) {
        val author = broadcast.authorId?.let { userRepository.findById(it).getOrNull() } ?: return
        val msg = "Рассылка №${broadcast.id} успешно запущена"
        bot.execute(
            SendMessage().also {
                it.chatId = author.tui!!
                it.text = msg
            },
        )
    }

    fun finishBroadcast(broadcast: Broadcast) {
        val finalBroadcast = broadcast.copy(
            isCompleted = true,
            finishTime = LocalDateTime.now(
                ZoneId.of("Europe/Moscow"),
            ),
        )

        broadcastRepository.save(finalBroadcast)
        val author = finalBroadcast.authorId?.let { userRepository.findById(it).getOrNull() } ?: return

        // TODO: нормальный формат вывода времени
        val msgText = "Рассылка №${finalBroadcast.id} успешно завершена\n" +
            "Число пользователей, получивших сообщение: ${finalBroadcast.receivedUsersId.size}\n" +
            "Старт рассылки: ${finalBroadcast.startTime}\n" +
            "Конец рассылки: ${finalBroadcast.finishTime}"

        bot.execute(
            SendMessage().also {
                it.chatId = author.tui!!
                it.text = msgText
            },
        )
    }

    private fun checkValidUser(user: User, broadcast: Broadcast): Boolean {
        return user.id !in broadcast.receivedUsersId && (
            user.categories.intersect(broadcast.categoriesId).isNotEmpty() ||
                broadcast.categoriesId.isEmpty()
            )
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun createChooseKeyboard(firstActiveBroadcast: Broadcast): InlineKeyboardMarkup {
        val keyboardList = buttonRepository
            .findAllValidButtonsForBroadcast(firstActiveBroadcast.id!!)
            .map {
                CallbackData(
                    callbackData = it.callbackData,
                    metaText = it.text,
                    metaUrl = it.link
                ).save()
            }

        val keyboard = keyboardList.map { callbackData ->
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
