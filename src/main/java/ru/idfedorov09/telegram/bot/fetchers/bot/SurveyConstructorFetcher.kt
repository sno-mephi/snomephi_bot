package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.GlobalConstants
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.*
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

@Component
class SurveyConstructorFetcher (
    private val callbackDataRepository: CallbackDataRepository,
    private val broadcastRepository: BroadcastRepository,
    private val messageSenderService: MessageSenderService,
    private val surveyQuestionRepository: SurveyQuestionRepository,
    private val surveyAnswerOptionRepository: SurveyAnswerOptionRepository,
    private val updatesUtil: UpdatesUtil,
    private val buttonRepository: ButtonRepository,
) : DefaultFetcher() {

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
    @InjectData
    @FetcherPerms(UserRole.MAILER)
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val params = Params(userActualizedInfo, update, updatesUtil)
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
                startsWith(TextCommands.SURVEY_CONSTRUCTOR()) -> createConsole(params)
                startsWith(TextCommands.SURVEY_QUESTION()) -> changeSurveyQuestion(params, text)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun changeSurveyQuestion(params: Params, textCommand: String) {
        params.userActualizedInfo.apply {
            val surveyQuestion = surveyQuestionRepository.findById(textCommand.split("_").last().toLong()).getOrNull()
            surveyQuestion?: return
            data?.surveyQuestionChangedId = surveyQuestion.id

            showSurveyQuestionConsole(params, surveyQuestion)
            deleteUpdateMessage()
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
            bcData =
                broadcastRepository.save(
                    Broadcast(
                        authorId = id,
                        isSurvey = true,
                    ),
                )
            val sent = messageSenderService.sendMessage(consoleMessageParams(params))
            bcData =
                bcData?.copy(
                    lastConsoleMessageId =  sent.messageId
                )

        }
    }

    private fun commonTextHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.SURVEY_CREATE_QUESTION -> enterQuestionText(params)
            LastUserActionType.SURVEY_QUESTION_CHOSEN_TYPE -> enterAnswerOptions(params)
            LastUserActionType.SURVEY_QUESTION_CHANGE_TEXT -> enterChangeQuestionText(params)
            LastUserActionType.SURVEY_QUESTION_CHANGE_ANSWER_OPTIONS -> enterChangeAnswerOption(params)
            LastUserActionType.SURVEY_CHANGE_STANDARD_ORDER_QUESTIONS -> enterChangedStandardOrder(params)
            LastUserActionType.SURVEY_CHANGE_START_TIME -> changeStartTime(params)
            else -> return
        }
    }

    private fun changeStartTime(params: Params) {
        params.userActualizedInfo.apply {
            val msgText = params.update.message.text.trim()
            val startTime =
                when {
                    msgText.matches(Regex("\\d{2}.\\d{2}.\\d{4} \\d{2}:\\d{2}")) -> resolveFullDate(msgText)
                    msgText.matches(Regex("\\d{2}:\\d{2}")) -> resolveShortDate(msgText)
                    else -> null
                } ?: run {
                    scheduleMessage(params, prefix = "Неверный формат даты и времени")
                    return
                }
            bcData =
                bcData?.copy(
                    startTime = startTime,
                    isScheduled = true,
                )

            lastUserActionType = LastUserActionType.DEFAULT
            deleteUpdateMessage()
            buildSurvey(params)
        }
    }

    private fun resolveFullDate(fullDateText: String) =
        LocalDateTime.parse(fullDateText, FORMATTER)
            .atZone(GlobalConstants.BOT_TIME_ZONE).toLocalDateTime()

    /**
     * Возвращает по сообщению формата HH:mm текущую дату с таким временем в LocalDateTime
     */
    private fun resolveShortDate(timeText: String): LocalDateTime {
        val nowDttm = LocalDateTime.now().atZone(GlobalConstants.BOT_TIME_ZONE).toLocalDateTime()
        val currentDate = nowDttm.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val formatString = "$currentDate $timeText"
        return LocalDateTime.parse(formatString, FORMATTER)
    }

    private fun enterChangedStandardOrder(params: Params) {
        params.apply {
            val surveyQuestionsId = update.message.text.split(", ").map { it.toLong() }
            val surveyQuestionsIdInDataBase = userActualizedInfo.bcData?.id?.let {
                surveyQuestionRepository.findAllSurveyQuestionByBroadcast(
                    it
                )
            }?.map { it.id } ?: return

            if (surveyQuestionsId.toSet() != surveyQuestionsIdInDataBase.toSet()) {
                val msgText = "Ошибка! Введите корректный порядок вопросов!"
                val back = CallbackData(callbackData = CallbackCommands.SURVEY_CHANGE_STANDARD_ORDER.data, metaText = "назад").save()
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = userActualizedInfo.tui,
                        messageId = userActualizedInfo.bcData?.lastConsoleMessageId,
                        text = msgText,
                        replyMarkup = createKeyboard(back)
                    )
                )
            } else {
                surveyQuestionsId.forEachIndexed() { count, it ->
                    val surveyQuestion = surveyQuestionRepository.findById(it).get()
                    surveyQuestionRepository.save(
                        surveyQuestion.copy(
                            surveyDepth = count.toLong(),
                            isFirstQuestion = count == 0,
                            isLastQuestion = count == surveyQuestionsId.size - 1
                        )
                    )
                }
                sendStartTimeMessage(params)
            }

            deleteUpdateMessage()
        }
    }

    private fun enterChangeQuestionText(params: Params) {
        params.apply {
            val surveyQuestion = getCurrentSurveyQuestion(params)?: return

            surveyQuestionRepository.save(
                surveyQuestion.copy(
                    text = update.message.text
                )
            )

            deleteUpdateMessage()
            showSurveyQuestionConsole(params, surveyQuestion)

            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun enterChangeAnswerOption(params: Params) {
        params.apply {
            val surveyQuestion = getCurrentSurveyQuestion(params)?: return

            surveyQuestion.id?.let { surveyAnswerOptionRepository.deleteAnswerOptionByQuestion(it) }


            update.message.text.split("\n").forEach{
                val surveyAnswerOption = SurveyAnswerOption(
                    optionText = it,
                    broadcastId = userActualizedInfo.bcData?.id,
                    surveyQuestionId = surveyQuestion.id
                ).save()
                CallbackData(
                    callbackData = CallbackCommands.SURVEY_USER_ANSWER.data,
                    metaText = it,
                    surveyAnswerOptionId = surveyAnswerOption.id
                ).save()
            }

            deleteUpdateMessage()
            showSurveyQuestionConsole(params, surveyQuestion)

            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun enterAnswerOptions(params: Params) {
        params.apply {
            update.message.text.split("\n").forEach{
                val surveyAnswerOption = SurveyAnswerOption(
                    optionText = it,
                    broadcastId = userActualizedInfo.bcData?.id,
                    surveyQuestionId = userActualizedInfo.surveyQuestionData?.id
                ).save()
                CallbackData(
                    callbackData = CallbackCommands.SURVEY_USER_ANSWER.data,
                    metaText = it,
                    surveyAnswerOptionId = surveyAnswerOption.id
                ).save()
            }
            showConsole(params)

            completeQuestion(params, true)
            userActualizedInfo.lastUserActionType = LastUserActionType.DEFAULT
            deleteUpdateMessage()
        }
    }

    private fun showSurveyQuestion(params: Params, surveyQuestion: SurveyQuestion){
        val callbackDataList = if (surveyQuestion.isMultiplyChoiceQuestion == true){
            surveyQuestion.id?.let {
                surveyAnswerOptionRepository.findAllSurveyAnswerOptionByQuestion(surveyQuestionId = it)
            }?.map {
                it.id?.let { id -> callbackDataRepository.findBySurveyAnswerOptionId(id) } ?: return
            } ?: run {
                //TODO: log
                return
            }
        } else {
            listOf()
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
                startsWith(CallbackCommands.SURVEY_SHOW_QUESTION_CONSOLE.data) -> getCurrentSurveyQuestion(params)?.let {
                    showSurveyQuestionConsole(params,
                        it
                    )
                }
                startsWith(CallbackCommands.SURVEY_BACK_TO_CONSOLE.data) -> showConsole(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_TEXT.data) -> surveyChangeText(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_TYPE.data) -> surveyChangeType(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_ANSWER_OPTION.data) -> surveyChangeAnswer(params)
                startsWith(CallbackCommands.SURVEY_DELETE_QUESTION.data) -> surveyQuestionDelete(params)
                startsWith(CallbackCommands.SURVEY_ORDER_QUESTION.data) -> createOrderQuestions(params)
                startsWith(CallbackCommands.SURVEY_STANDARD_ORDER.data) -> standardOrder(params)
                startsWith(CallbackCommands.SURVEY_DO_NOT_CHANGE_STANDARD_ORDER.data) -> standardDoNotChangeOrderConfirm(params)
                startsWith(CallbackCommands.SURVEY_CHANGE_STANDARD_ORDER.data) -> standardChangeOrder(params)
                startsWith(CallbackCommands.SURVEY_START_NOW.data) -> buildSurvey(params)
                startsWith(CallbackCommands.SURVEY_SCHEDULE_SENDING.data) -> scheduleMessage(params)
            }
        }
    }

    private fun scheduleMessage(params: Params, prefix: String? = null) {
        params.userActualizedInfo.apply {
            val msgStart = prefix?.let { "$prefix\n" } ?: ""
            val msgText = msgStart  +
                    "\uD83D\uDD57 Отправь время запуска рассылки в формате <b><i>ДД.ММ.ГГГГ ЧЧ:ММ</i></b>" +
                        " или напиши время рассыли в формате <b><i>ЧЧ:ММ</i></b>, " +
                        "если хочешь разослать <b><i><u>сегодня</u></i></b>\n\n" +
                        "Например, если ты отправишь\n<pre>24.06.2077 19:25</pre>\nто рассылка начнется " +
                        "24 июня 2077 года в 19:25, а если \n<pre>23:50</pre>\nто рассылка начнется <u>сегодня</u> в 23:50"
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = msgText,
                    parseMode = ParseMode.HTML,
                    messageId = bcData?.lastConsoleMessageId
                ),
            )
            lastUserActionType = LastUserActionType.SURVEY_CHANGE_START_TIME
        }
    }

    private fun sendStartTimeMessage(params: Params){
        params.userActualizedInfo.apply {
            val startNow = CallbackData(callbackData = CallbackCommands.SURVEY_START_NOW.data, metaText = "Отправить сейчас").save()
            val scheduleSending = CallbackData(callbackData = CallbackCommands.SURVEY_SCHEDULE_SENDING.data, metaText = "Отложить отправку").save()
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = "Опрос успешно создан! Выберите дальнейшее действие",
                    messageId = bcData?.lastConsoleMessageId,
                    replyMarkup = createKeyboard(startNow, scheduleSending),
                )
            )
            lastUserActionType = LastUserActionType.DEFAULT
        }
    }

    private fun buildSurvey(params: Params){
        params.userActualizedInfo.apply {
            val nowTime = params.updatesUtil.getDate(params.update)
                ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            val sendTime = bcData?.startTime ?: nowTime
            val isScheduled = bcData?.isScheduled ?: false
            bcData =
                bcData?.copy(
                    isBuilt = true,
                    startTime = sendTime,
                    isScheduled = isScheduled,
                    lastConsoleMessageId = null,
                    text = "Доброго времени суток, предлагаем вам пройти небольшой опрос.\n\n" +
                            "При нажатии кнопки Начать, диалог будет автоматически завершен!"
                )
            lastUserActionType = LastUserActionType.DEFAULT

            val msgText = if (isScheduled) {
                "Отложенный опрос успешно создан"
            } else {
                "Опрос успешно создан, начинается рассылка..."
            }
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = msgText,
                    messageId = bcData?.lastConsoleMessageId
                )
            )

            Button(
                text = "Начать",
                callbackData = CallbackCommands.SURVEY_USER_START.data + "_${bcData?.id}",
                authorId = id,
                broadcastId = bcData?.id
            ).save()
        }
    }

    private fun standardDoNotChangeOrderConfirm(params: Params) {
        params.userActualizedInfo.apply {
            val surveyQuestions = bcData?.id?.let { surveyQuestionRepository.findAllSurveyQuestionByBroadcast(it) }

            surveyQuestions?.forEachIndexed  {count, it ->
                    surveyQuestionRepository.save(
                        it.copy(
                            surveyDepth = count.toLong(),
                            isFirstQuestion = count == 0,
                            isLastQuestion = count == surveyQuestions.size - 1
                        )
                    )
                }
            sendStartTimeMessage(params)
        }
    }

    private fun standardChangeOrder(params: Params) {
        params.userActualizedInfo.apply {
            val allQuestion = bcData?.id?.let { surveyQuestionRepository.findAllSurveyQuestionByBroadcast(it) }?.map {
                it.text + " /show_question_" + it.id.toString()
            }?.joinToString(separator = "\n\n") { it }
            val mailText = "<b>Ниже перечислен список вопросов в опросе</b>\n\n" +
                    "В суффиксе каждого вопроса указан его уникальный номер\n"+
                    "Пожалуйста, перечислите через запятую порядок этих вопросов\n\n\n"+
                    "$allQuestion"
            val backToConsole = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "вернуться").save()

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    messageId = bcData?.lastConsoleMessageId,
                    text = mailText,
                    replyMarkup = createKeyboard(backToConsole),
                    parseMode = ParseMode.HTML,
                )
            )

            lastUserActionType = LastUserActionType.SURVEY_CHANGE_STANDARD_ORDER_QUESTIONS
        }
    }

    private fun standardOrder(params: Params) {
        params.userActualizedInfo.apply {
            val msgText = "Вы можете настроить порядок вопрос или отправить их в том порядке, в каком вы их добавляли"
            val custom = CallbackData(callbackData = CallbackCommands.SURVEY_CHANGE_STANDARD_ORDER.data, metaText = "Настроить порядок").save()

            //TODO: Проверка, отложка?
            val standard = CallbackData(callbackData = CallbackCommands.SURVEY_DO_NOT_CHANGE_STANDARD_ORDER.data, metaText = "Оставить порядок").save()
            val back = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "Назад").save()

            messageSenderService.editMessage(
                MessageParams(
                    messageId = bcData?.lastConsoleMessageId,
                    chatId = tui,
                    text = msgText,
                    replyMarkup = createKeyboard(custom, standard, back)
                )
            )
        }
    }

    private fun createOrderQuestions(params: Params) {
        params.userActualizedInfo.apply {
            if (checkValidateSurvey(params) == true) {
                val msgText = "Выберите тип порядка вопросов"

                val standard = CallbackData(callbackData = CallbackCommands.SURVEY_STANDARD_ORDER.data, metaText = "Последовательный").save()

                //TODO: нужно сделать кастомные вопросы
                val custom = CallbackData(callbackData = CallbackCommands.SURVEY_CUSTOM_ORDER.data, metaText = "Кастомный (не работает)").save()
                val back = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "Назад").save()

                messageSenderService.editMessage(
                    MessageParams(
                        chatId = tui,
                        text = msgText,
                        messageId = bcData?.lastConsoleMessageId,
                        replyMarkup = createKeyboard(standard, custom, back)
                    )
                )
            } else {
                val msgText = "Сначала добавте хотя бы один вопрос!"
                val back = CallbackData(callbackData = CallbackCommands.SURVEY_BACK_TO_CONSOLE.data, metaText = "Назад").save()
                messageSenderService.editMessage(
                    MessageParams(
                        chatId = tui,
                        text = msgText,
                        messageId = bcData?.lastConsoleMessageId,
                        replyMarkup = createKeyboard(back)
                    )
                )
            }
        }
    }

    private fun checkValidateSurvey(params: Params) : Boolean?{
        params.userActualizedInfo.apply {
            return bcData?.id?.let { surveyQuestionRepository.findAllSurveyQuestionByBroadcast(it).isNotEmpty() }
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
            val surveyQuestion = getCurrentSurveyQuestion(params)?: return

            val flg = surveyQuestion.isMultiplyChoiceQuestion?: return
            val messageText = "Тип вопроса успешно изменен\n\n" + if (flg) {
                "Варианты ответов были автоматически удалены"
            } else {
                "Запишите одним сообщение варианты ответов, разделяя их новой строкой"
            }
            val keyboard = mutableListOf<CallbackData>()
            if (flg) keyboard.add(CallbackData(callbackData = CallbackCommands.SURVEY_SHOW_QUESTION_CONSOLE.data, metaText = "назад").save())

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = messageText,
                    replyMarkup = createKeyboard(*keyboard.toTypedArray()),
                    messageId = bcData?.lastConsoleMessageId,
                )
            )

            surveyQuestionRepository.save(
                surveyQuestion.copy(
                    isMultiplyChoiceQuestion = !flg
                )
            )
            deleteMessageWithSurveyQuestion(params)
            lastUserActionType = LastUserActionType.SURVEY_QUESTION_CHANGE_ANSWER_OPTIONS
        }
    }

    private fun surveyChangeAnswer(params: Params) {
        params.userActualizedInfo.apply {
            deleteMessageWithSurveyQuestion(params)
            val messageText = "Запишите одним сообщение варианты ответов, разделяя их новой строкой"
            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    messageId = bcData?.lastConsoleMessageId,
                    text = messageText,
                )
            )
            lastUserActionType = LastUserActionType.SURVEY_QUESTION_CHANGE_ANSWER_OPTIONS
        }
    }

    private fun surveyQuestionDelete(params: Params){
        params.userActualizedInfo.apply {
            val surveyQuestion = getCurrentSurveyQuestion(params)?: return

            surveyQuestionRepository.save(
                surveyQuestion.copy(
                    isDeleted = true,
                )
            )

            deleteMessageWithSurveyQuestion(params)
            showConsole(params)
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
                surveyQuestionData?.copy(
                    isMultiplyChoiceQuestion = isMultiplyChoiceQuestion,
                    isBuilt = true,
                )
            lastUserActionType = LastUserActionType.DEFAULT
            showConsole(params)
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
            lastUserActionType = LastUserActionType.DEFAULT
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

    private fun getCurrentSurveyQuestion(params: Params) : SurveyQuestion?{
        params.userActualizedInfo.apply {
            return data?.surveyQuestionChangedId?.let { surveyQuestionRepository.findById(it).get() }
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

    private fun Button.save() = buttonRepository.save(this)

    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
        val updatesUtil: UpdatesUtil,
    )
}