package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.BroadcastRepository
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.SurveyAnswerOptionRepository
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
    private val surveyAnswerOptionRepository: SurveyAnswerOptionRepository
) : DefaultFetcher() {
    @InjectData
    @FetcherPerms(UserRole.MAILER)
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
                startsWith(TextCommands.SURVEY_QUESTION()) -> changeSurveyQuestion(params, text)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun changeSurveyQuestion(params: Params, textCommand: String) {
        params.userActualizedInfo.apply {
            val surveyQuestion = surveyQuestionRepository.findById(textCommand.split("_").last().toLong()).getOrNull()
            surveyQuestion?: return
            surveyQuestionData = surveyQuestion

            showSurveyQuestionConsole(params, surveyQuestion)
        }
    }

    private fun showSurveyQuestionConsole(params: Params, surveyQuestion: SurveyQuestion) {
        params.userActualizedInfo.apply {
            val messageText = "Вы можете изменить этот вопрос"

            val changeText = CallbackData(callbackData = CallbackCommands.SURVEY_CHANGE_TEXT.data , metaText = "Изменить текст вопроса").save()
            val changeType = CallbackData(callbackData = CallbackCommands.SURVEY_CHANGE_TYPE.data , metaText = "Изменить тип вопроса").save()
            val changeAnswerOption = CallbackData(callbackData = CallbackCommands.SURVEY_CHANGE_ANSWER_OPTION.data, metaText = "Изменить варианты ответа").save()
            val deleteQuestion = CallbackData(callbackData = CallbackCommands.SURVEY_DELETE_QUESTION.data, metaText = "Удалить вопрос").save()
            val backToConsole = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "Назад").save()

            val callbackDataList = mutableListOf(changeText, changeType)
            if (surveyQuestion.isMultiplyChoiceQuestion == true) callbackDataList.add(changeAnswerOption)
            callbackDataList.add(deleteQuestion)
            callbackDataList.add(backToConsole)

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = messageText,
                    replyMarkup = createKeyboard(*callbackDataList.toTypedArray()),
                    messageId = bcData?.lastConsoleMessageId,
                )
            )
            showSurveyQuestion(params, surveyQuestion)
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
            LastUserActionType.SURVEY_CREATE_QUESTION -> enterQuestionText(params)
            LastUserActionType.SURVEY_QUESTION_CHOSEN_TYPE -> enterAnswerOptions(params)
            LastUserActionType.SURVEY_QUESTION_CHANGE_TEXT -> enterChangeQuestionText(params)
            LastUserActionType.SURVEY_QUESTION_CHANGE_ANSWER_OPTIONS -> enterChangeAnswerOption(params)
            else -> return
        }
    }

    private fun enterChangeAnswerOption(params: Params) {
        params.apply {
            userActualizedInfo.surveyQuestionData?: return
            showSurveyQuestionConsole(params, userActualizedInfo.surveyQuestionData!!)
            deleteUpdateMessage()

            userActualizedInfo.surveyQuestionData =
                userActualizedInfo.surveyQuestionData?.copy(
                    text = update.message.text
                )
            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT

        }
    }

    private fun enterChangeQuestionText(params: Params) {
        params.apply {
            userActualizedInfo.surveyQuestionData?: return
            showSurveyQuestionConsole(params, userActualizedInfo.surveyQuestionData!!)
            deleteUpdateMessage()

            update.message.text.split("\n").forEach{
                SurveyAnswerOption(
                    optionText = it,
                    broadcastId = userActualizedInfo.bcData?.id,
                    surveyQuestionId = userActualizedInfo.surveyQuestionData?.id
                ).save()
            }

            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun enterAnswerOptions(params: Params) {
        params.apply {
            update.message.text.split("\n").forEach{
                SurveyAnswerOption(
                    optionText = it,
                    broadcastId = userActualizedInfo.bcData?.id,
                    surveyQuestionId = userActualizedInfo.surveyQuestionData?.id
                ).save()
            }
            showConsole(params)

            completeQuestion(params, true)
            userActualizedInfo.surveyQuestionData?.let { showSurveyQuestion(params, it) }
            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
            deleteUpdateMessage()
        }
    }

    private fun showSurveyQuestion(params: Params, surveyQuestion: SurveyQuestion){
        val callbackDataList = surveyQuestion.id?.let {
            surveyAnswerOptionRepository.findAllSurveyAnswerOptionByQuestion(surveyQuestionId = it)
        }?.map{ callbackDataRepository.findBySurveyAnswerOptionId(it) } ?: run {
            // TODO: log
            return
        }

        deleteMessageWithSurveyQuestion(params)

        val keyboard = createKeyboard(*callbackDataList.toTypedArray())

        params.userActualizedInfo.apply {
            val sent = messageSenderService.sendMessage(
                MessageParams(
                    chatId = tui,
                    text = surveyQuestion.text,
                    replyMarkup = keyboard,
                )
            )
            data?.surveyQuestionMessageId = sent.messageId
        }
    }

    /** Удаляет сообщение с примером вопроса, если оно есть **/

    private fun deleteMessageWithSurveyQuestion(params: Params){
        params.userActualizedInfo.apply {
            if (data?.surveyQuestionMessageId != null) {
                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = tui,
                        messageId = data!!.surveyQuestionMessageId
                    )
                )
                data!!.surveyQuestionMessageId = null
            }
        }
    }

    private fun enterQuestionText(params: Params) {
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
            deleteUpdateMessage()
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
                startsWith(CallbackCommands.SURVEY_TEXT_QUESTION.data) -> completeQuestion(params, false)
                startsWith(CallbackCommands.SURVEY_MULTIPLY_CHOICE_QUESTION.data) -> createAnswerOptions(params)
                startsWith(CallbackCommands.SURVEY_SHOW_QUESTIONS.data) -> showQuestions(params)
                startsWith(CallbackCommands.SURVEY_BACK_TO_CONSOLE.data) -> showConsole(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_TEXT.data) -> surveyChangeText(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_TYPE.data) -> surveyChangeType(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_ANSWER_OPTION.data) -> surveyChangeAnswer(params)
                startsWith(CallbackCommands.SURVEY_DELETE_QUESTION.data) -> surveyQuestionDelete(params)
            }
        }
    }

    private fun surveyChangeText(params: Params) {
        params.userActualizedInfo.apply {
            deleteMessageWithSurveyQuestion(params)
            val messageText = "Введите, пожалуйста новый текст вопроса"
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    messageId = bcData?.lastConsoleMessageId,
                    text = messageText,
                )
            )
            lastUserActionType = LastUserActionType.SURVEY_QUESTION_CHANGE_TEXT
        }
    }

    private fun surveyChangeType(params: Params) {
        params.userActualizedInfo.apply {
            val flg = surveyQuestionData?.isMultiplyChoiceQuestion?: return
            val messageText = "Тип вопроса успешно изменен\n\n" + if (flg) {
                "Варианты ответов теперь не отображаются, но будут возвращены, если вы снова измените тип вопроса"
            } else {
                "Запишите одним сообщение варианты ответов, разделяя их новой строкой"
            }
            val keyboard = mutableListOf<CallbackData>()
            if (flg) keyboard.add(CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "назад").save())

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = messageText,
                    replyMarkup = createKeyboard(*keyboard.toTypedArray()),
                    messageId = bcData?.lastConsoleMessageId,
                )
            )

            surveyQuestionData =
                surveyQuestionData!!.copy(
                    isMultiplyChoiceQuestion = !flg
                )
        }
    }

    private fun surveyChangeAnswer(params: Params) {
        TODO("Not yet implemented")
    }

    private fun surveyQuestionDelete(params: Params){
        params.userActualizedInfo.apply {
            surveyQuestionData =
                surveyQuestionData?.copy(
                    isDeleted = true,
                )
            deleteMessageWithSurveyQuestion(params)
        }
    }

    private fun showQuestions(params: Params) {
        params.userActualizedInfo.apply {
            val allQuestion = bcData?.id?.let { surveyQuestionRepository.findAllSurveyQuestionByBroadcast(it) }?.map {
                it.text + " /show_question_" + it.id.toString()
            }?.joinToString(separator = "\n\n") { it }
            val mailText = "<b>Список вопросов в опросе:</b>\n\n$allQuestion"
            val backToConsole = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "вернуться").save()
            messageSenderService.editMessage(
                MessageParams(
                    messageId = bcData?.lastConsoleMessageId,
                    chatId = tui,
                    text = mailText,
                    parseMode = ParseMode.HTML,
                    replyMarkup = createKeyboard(backToConsole),
                ),
            )
        }
    }

    private fun createAnswerOptions(params: Params) {
        params.userActualizedInfo.apply {
            val messageText = "Запишите одним сообщение варианты ответов, разделяя их новой строкой"

            messageSenderService.editMessage(
                MessageParams(
                    messageId = bcData?.lastConsoleMessageId,
                    text = messageText,
                    chatId = tui,
                )
            )

            lastUserActionType = LastUserActionType.SURVEY_QUESTION_CHOSEN_TYPE
        }
    }

    private fun completeQuestion(params: Params, isMultiplyChoiceQuestion: Boolean) {
        params.userActualizedInfo.apply {
            surveyQuestionData =
                surveyQuestionRepository.save(
                    SurveyQuestion(
                        isMultiplyChoiceQuestion = isMultiplyChoiceQuestion,
                        isBuilt = true,
                    )
                )
            lastUserActionType = LastUserActionType.DEFAULT
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

            deleteMessageWithSurveyQuestion(params)
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

    private fun SurveyAnswerOption.save() = surveyAnswerOptionRepository.save(this)

    private fun CallbackData.save() = callbackDataRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}