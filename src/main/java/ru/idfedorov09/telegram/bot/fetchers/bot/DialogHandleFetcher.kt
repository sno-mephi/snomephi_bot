package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.QuestionStatus
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.Quest
import ru.idfedorov09.telegram.bot.data.model.QuestDialogMessage
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.executor.Executor
import ru.idfedorov09.telegram.bot.repo.QuestDialogMessageRepository
import ru.idfedorov09.telegram.bot.repo.QuestRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Фетчер отвечающий за общение челиков в режиме активного диалога
 */
@Component
class DialogHandleFetcher(
    private val bot: Executor,
    private val questRepository: QuestRepository,
    private val questDialogMessageRepository: QuestDialogMessageRepository,
    private val userRepository: UserRepository,
) : GeneralFetcher() {

    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        if (!(update.hasMessage() && update.message.hasText())) return

        // если пользователь не в активном диалоге то скипаем фетчер
        if (userActualizedInfo.activeQuest == null) return

        // если апдейт из беседы, то игнорим
        if (update.message.chatId.toString() != userActualizedInfo.tui) return

        val messageText = update.message.text
        val quest = userActualizedInfo.activeQuest
        val author = userRepository.findById(quest.authorId!!).get()
        val responder = userRepository.findById(quest.responderId!!).get()
        val isByQuestionAuthor = author.tui == userActualizedInfo.tui

        val params = Params(
            messageText = messageText,
            quest = quest,
            author = author,
            responder = responder,
        )

        // если пришла команда - обрабатываем как команду, остальное скипаем
        if (TextCommands.isTextCommand(messageText)) {
            handleCommands(params)
            return
        }

        val questDialogMessage = QuestDialogMessage(
            questId = quest.id,
            isByQuestionAuthor = isByQuestionAuthor,
            authorId = userActualizedInfo.id,
            messageText = messageText,
            messageId = update.message.messageId,
        ).let { questDialogMessageRepository.save(it) }
        quest.dialogHistory.add(questDialogMessage.id!!)
        questRepository.save(quest)

        // TODO: добавить поддержку картинок, файлов, HTML/MARKDOWN-разметки
        bot.execute(
            SendMessage().also {
                it.chatId = if (isByQuestionAuthor) responder.tui!! else author.tui!!
                it.text = messageText
            },
        )
    }

    private fun handleCommands(params: Params) {
        when (params.messageText) {
            TextCommands.QUEST_DIALOG_CLOSE.commandText -> closeDialog(params)
        }
    }

    private fun closeDialog(params: Params) {
        questRepository.save(
            params.quest.copy(
                questionStatus = QuestionStatus.CLOSED,
            ),
        )

        bot.execute(
            SendMessage().also {
                it.chatId = params.author.tui!!
                it.text = "_⚠\uFE0F Оператор завершил диалог\\._"
                it.parseMode = ParseMode.MARKDOWNV2
            },
        )

        bot.execute(
            SendMessage().also {
                it.chatId = params.responder.tui!!
                it.text = "Спасибо за обратную связь\\! *Диалог завершен\\.*"
                it.parseMode = ParseMode.MARKDOWNV2
            },
        )
    }

    /**
     * Вспомогательный класс для передачи параметров
     */
    private data class Params(
        val messageText: String,
        val quest: Quest,
        val author: User,
        val responder: User,
    )
}
