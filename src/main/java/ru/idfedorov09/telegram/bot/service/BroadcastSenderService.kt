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
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
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

    fun sendBroadcast(user: User, broadcast: Broadcast, shouldAddToReceived: Boolean = true) {
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
        broadcastRepository.save(
            broadcast.copy(
                isCompleted = true,
                finishTime = LocalDateTime.now(
                    ZoneId.of("Europe/Moscow"),
                ),
            ),
        )
        val author = broadcast.authorId?.let { userRepository.findById(it).getOrNull() } ?: return
        val msgText = "Рассылка №${broadcast.id} успешно завершена\n" +
            "Число пользователей, получивших сообщение: ${broadcast.receivedUsersId.size}\n" +
            "Старт рассылки: ${broadcast.startTime}\n" +
            "Конец рассылки: ${broadcast.finishTime}"
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
        val keyboardList = mutableListOf<List<InlineKeyboardButton>>()
        buttonRepository.findAllById(firstActiveBroadcast.buttonsId).forEach { button ->
            keyboardList.add(
                listOf(
                    InlineKeyboardButton("${button.text}").also {
                        it.url = button.link
                        it.callbackData = button.callbackData
                    },
                ),
            )
        }
        return createKeyboard(keyboardList)
    }
}
