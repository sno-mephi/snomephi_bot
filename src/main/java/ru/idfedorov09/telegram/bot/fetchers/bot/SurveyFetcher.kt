package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.SurveyQuestionRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.jvm.optionals.getOrNull

@Component
class SurveyFetcher (
    private val callbackDataRepository: CallbackDataRepository,
    private val broadcastRepository: BroadcastRepository,
    private val messageSenderService: MessageSenderService,
    private val surveyQuestionRepository: SurveyQuestionRepository,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val params = Params(userActualizedInfo, update)
        when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> return
        }
    }

    private fun textCommandsHandler(params: Params){
        val text = params.update.message.text

        text.apply {
            when {
                startsWith(TextCommands.BROADCAST_CONSTRUCTOR()) -> createConsole(params)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun createConsole(params: Params) {
        params.userActualizedInfo.apply {
            val sent = messageSenderService.sendMessage(consoleMessageParams(params))

            bcData =
                broadcastRepository.save(
                    Broadcast(
                        authorId = id,
                        isSurvey = true,
                        lastConsoleMessageId = sent.messageId
                    ),
                )
        }
    }

    private fun commonTextHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.SURVEY_CREATE_QUESTION -> enterAnswerText(params)
            else -> return
        }
    }

    private fun enterAnswerText(params: Params) {
        params.apply {
            val messageText = "Выберите тип опроса"

            val textQuestion = CallbackData(callbackData = CallbackCommands.SURVEY_TEXT_QUESTION.data, metaText = "текстовый вопрос").save()
            val multiplyChoiceQuestion = CallbackData(callbackData = CallbackCommands.SURVEY_MULTIPLY_CHOICE_QUESTION.data, metaText = "вопрос с выбором ответа").save()
            messageSenderService.editMessage(
                MessageParams(
                    messageId = userActualizedInfo.bcData?.lastConsoleMessageId,
                    text = messageText,
                    chatId = userActualizedInfo.tui,
                    replyMarkup = createKeyboard(textQuestion, multiplyChoiceQuestion)
                )
            )

            userActualizedInfo.surveyQuestionData =
                userActualizedInfo.surveyQuestionData?.copy(
                    text = update.message.text
                )
        }
    }

    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.SURVEY_CANCEL.data) -> surveyCancel(params)
                startsWith(CallbackCommands.SURVEY_NEW_QUESTION.data) -> createQuestion(params)
                startsWith(CallbackCommands.SURVEY_TEXT_QUESTION.data) ->
            }
        }
    }

    private fun createQuestion(params: Params) {
        params.userActualizedInfo.apply {
            val messageText = "Напишите пожалуйста текст вопроса"
            messageSenderService.editMessage(
                MessageParams(
                    messageId = bcData?.lastConsoleMessageId,
                    text = messageText,
                    chatId = tui,
                )
            )

            surveyQuestionData =
                surveyQuestionRepository.save(
                    SurveyQuestion(
                        broadcastId = bcData?.id,
                    )
                )
            lastUserActionType = LastUserActionType.SURVEY_CREATE_QUESTION
        }
    }

    private fun surveyCancel(params: Params) {
        params.userActualizedInfo.apply {
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = tui,
                    messageId = bcData?.lastConsoleMessageId!!,
                ),
            )
            bcData?.let {
                broadcastRepository.save(
                    it.copy(
                        isDeleted = true,
                    ),
                )
            }
            bcData = null
        }
    }

    private fun showConsole(params: Params) {
        params.userActualizedInfo.apply {
            bcData?: return
            messageSenderService.editMessage(consoleMessageParams(params, bcData!!.lastConsoleMessageId))
        }
    }

    private fun consoleMessageParams(params: Params, messageId: Int? = null) : MessageParams {
        params.userActualizedInfo.apply {
            val messageText = "<b>Конструктор рассылки</b>\n\nВыберите дальнейшее действие"

            val createNewQuestion = CallbackData(callbackData = CallbackCommands.SURVEY_NEW_QUESTION.data, metaText = "Добавить новый вопрос").save()
            val showQuestions = CallbackData(callbackData = CallbackCommands.SURVEY_SHOW_QUESTIONS.data, metaText = "Показать список вопросов").save()
            val orderQuestions = CallbackData(callbackData = CallbackCommands.SURVEY_ORDER_QUESTION.data, metaText = "Выбрать порядок вопросов").save()
            val cancelButton = CallbackData(callbackData = CallbackCommands.SURVEY_CANCEL.data, metaText = "Отмена").save()

            return MessageParams(
                text = messageText,
                parseMode = ParseMode.HTML,
                replyMarkup = createKeyboard(createNewQuestion, showQuestions, orderQuestions, cancelButton),
                chatId = tui,
                disableWebPagePreview = !bcData!!.shouldShowWebPreview,
                messageId = messageId,
            )
        }
    }

    private fun createKeyboard(vararg callbackData: CallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*callbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}