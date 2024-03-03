package ru.idfedorov09.telegram.bot.util
import org.jvnet.hk2.annotations.Service
import org.springframework.scheduling.annotation.Scheduled
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.model.Broadcast
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.ButtonRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import java.time.LocalDateTime
import java.time.ZoneId

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
        val firstUser = userRepository.findAll().firstOrNull {
            it.id !in firstActiveBroadcast.receivedUsersId &&
                (
                    it.categories.intersect(firstActiveBroadcast.categoriesId).isEmpty() ||
                        firstActiveBroadcast.categoriesId.isEmpty()
                    )
        } ?: run {
            broadcastRepository.save(
                firstActiveBroadcast.copy(
                    isCompleted = true,
                    finishTime = LocalDateTime.now(
                        ZoneId.of("Europe/Moscow"),
                    ),
                ),
            )
            return
        }
        if (firstActiveBroadcast.imageHash == null) {
            bot.execute(
                SendMessage().also {
                    it.chatId = firstUser.tui.toString()
                    it.text = firstActiveBroadcast.text.toString()
                    it.replyMarkup = createChooseKeyboard(firstActiveBroadcast)
                },
            )
        } else {
            bot.execute(
                SendPhoto().also {
                    it.chatId = firstUser.tui.toString()
                    it.photo = InputFile(firstActiveBroadcast.imageHash)
                    it.replyMarkup = createChooseKeyboard(firstActiveBroadcast)
                },
            )
        }
        firstUser.id?.let { firstActiveBroadcast.receivedUsersId.add(it) }
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
