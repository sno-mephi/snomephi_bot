package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.base.executor.Executor
import ru.idfedorov09.telegram.bot.base.util.UpdatesUtil
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.model.*
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.*
import ru.idfedorov09.telegram.bot.service.DialogService
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.mephi.sno.libs.flow.belly.InjectData
import java.time.Instant
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull

@Component
class SurveyAnswerFetcher (
    private val questSegmentRepository: QuestSegmentRepository,
    private val updatesUtil: UpdatesUtil,
    private val ECallbackDataRepository: ECallbackDataRepository,
    private val surveyAnswerRepository: SurveyAnswerRepository,
    private val surveyQuestionRepository: SurveyQuestionRepository,
    private val dialogService: DialogService,
    private val userRepository: UserRepository,
    private val messageSenderService: MessageSenderService,
    private val surveyAnswerOptionRepository: SurveyAnswerOptionRepository,
    private val bot: Executor,
) : DefaultFetcher() {
    @InjectData
    fun doFetch(
        update: Update,
        userActualizedInfo: UserActualizedInfo,
    ) {
        val params = Params(userActualizedInfo, update)
        when {
            update.hasMessage() && update.message.hasText() -> commonTextHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(params)
            else -> return
        }
    }

    private fun commonTextHandler(params: Params) {
        when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.SURVEY_START_ANSWER -> enterTextAnswer(params)
            else -> return
        }
    }

    private fun enterTextAnswer(params: Params) {
        params.apply {
            val nextQuestion = userActualizedInfo.surveyId?.let { userActualizedInfo.currentSurveyQuestionNumber?.let { it1 ->
                surveyQuestionRepository.findQuestionByBroadcastAndNumber(it,
                    it1
                )
            } } ?: return
            val text = update.message.text
            surveyAnswerRepository.save(
                SurveyAnswer(
                    userId = userActualizedInfo.id,
                    answer = text,
                    broadcastId = userActualizedInfo.surveyId,
                    surveyQuestionId = nextQuestion.id,
                    answerTime =
                        updatesUtil.getDate(update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
                )
            )
            deleteUpdateMessage()
            userActualizedInfo.currentSurveyQuestionNumber = userActualizedInfo.currentSurveyQuestionNumber!! + 1
            sendNextQuestion(params)
        }
    }

    private fun callbackQueryHandler(params: Params) {
        val callbackId = params.update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return
        val callbackData = ECallbackDataRepository.findById(callbackId).getOrNull() ?: return

        callbackData.callbackData?.apply {
            when {
                startsWith(CallbackCommands.SURVEY_USER_ANSWER.data) -> userClickButton(params, callbackData)
                startsWith(CallbackCommands.SURVEY_USER_START.data) -> userStartSurvey(params, this)
            }
        }
    }

    private fun userClickButton(params: Params, ECallbackData: ECallbackData) {
        params.userActualizedInfo.apply {
            val answer = ECallbackData.surveyAnswerOptionId?.let { surveyAnswerOptionRepository.findById(it).get().optionText }

            val nextQuestion = surveyId?.let { currentSurveyQuestionNumber?.let { it1 ->
                surveyQuestionRepository.findQuestionByBroadcastAndNumber(it,
                    it1
                )
            } } ?: return

            surveyAnswerRepository.save(
                SurveyAnswer(
                    userId = id,
                    answer = answer,
                    broadcastId = surveyId,
                    surveyQuestionId = nextQuestion.id,
                    answerTime =
                    updatesUtil.getDate(params.update)
                        ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
                )
            )
            currentSurveyQuestionNumber = currentSurveyQuestionNumber!! + 1
            sendNextQuestion(params)

        }
    }

    private fun userStartSurvey(params: Params, callbackData: String) {
        params.userActualizedInfo.apply {
            if (surveyId != null) {
                val callbackAnswer =
                    AnswerCallbackQuery().also {
                        it.text = "Пожалуйста, пройдите предыдущий опрос перед тем, как начать новый.\n" +
                                "Бот уже отправил Вам последний вопрос еще раз."
                        it.callbackQueryId = params.update.callbackQuery.id
                        it.showAlert = true
                    }
                 bot.execute(callbackAnswer)

                messageSenderService.deleteMessage(
                    MessageParams(
                        chatId = tui,
                        messageId = data?.surveyUserQuestionMessageId
                    )
                )
                val sent = messageSenderService.sendMessage(
                    MessageParams(
                        chatId = tui,
                        text = "Пожалуйста, подождите...",
                    )
                )

                data?.surveyUserQuestionMessageId = sent.messageId
                val lastSurvey = data?.lastSurveyQuestionId?.let {
                    surveyQuestionRepository.findById(it).getOrNull()
                } ?: run {
                    // TODO: log error
                    return
                }

                sendQuestion(lastSurvey, params)
                return
            }
            surveyId = callbackData.split("_").last().toLong()
            lastUserActionType = LastUserActionType.SURVEY_START_ANSWER
            currentSurveyQuestionNumber = 0
            data?.surveyUserQuestionMessageId = params.update.callbackQuery.message.messageId
            closedDialog(params)

            sendNextQuestion(params)
        }
    }

    private fun sendNextQuestion(params: Params) {
        params.userActualizedInfo.apply {
            val nextQuestion = surveyId?.let { currentSurveyQuestionNumber?.let { it1 ->
                surveyQuestionRepository.findQuestionByBroadcastAndNumber(it,
                    it1
                )
            } } ?: run {
                completeSurvey(params)
                return
            }

            sendQuestion(nextQuestion, params)
        }
    }

    private fun sendQuestion(question: SurveyQuestion, params: Params) {
        params.userActualizedInfo.apply {
            val callbackDataList = if (question.isMultiplyChoiceQuestion == true){
                question.id?.let {
                    surveyAnswerOptionRepository.findAllSurveyAnswerOptionByQuestion(surveyQuestionId = it)
                }?.map {
                    it.id?.let { id -> ECallbackDataRepository.findBySurveyAnswerOptionId(id) } ?: return
                } ?: run {
                    //TODO: log
                    return
                }
            } else {
                listOf()
            }

            data ?: run { data = UserData() }
            data?.lastSurveyQuestionId = question.id

            messageSenderService.editMessage(
                MessageParams(
                    chatId = tui,
                    text = question.text,
                    messageId = data?.surveyUserQuestionMessageId,
                    replyMarkup = createKeyboard(*callbackDataList.toTypedArray())
                )
            )
        }
    }

    private fun completeSurvey(params: Params) {
        params.userActualizedInfo.apply {
            messageSenderService.editMessage(
                MessageParams(
                    messageId = data?.surveyUserQuestionMessageId,
                    chatId = tui,
                    text = "Спасибо, что приняли участие в опросе!",
                )
            )
            surveyId = null
            lastUserActionType = LastUserActionType.DEFAULT
            currentSurveyQuestionNumber = null
            data?.surveyUserQuestionMessageId = null
        }
    }

    private fun closedDialog(params: Params) {
        params.apply {
            val quest = userActualizedInfo.activeQuestDialog?: return
            val author = userRepository.findActiveUsersById(quest.authorId!!)!!
            val isByQuestionAuthor = author.tui == userActualizedInfo.tui
            val messageTime =
                updatesUtil.getDate(update)
                    ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime() }
            val segment = quest.lastQuestSegmentId?.let { questSegmentRepository.findById(it).get() }

            val responder = userRepository.findActiveUsersById(segment?.responderId!!)!!


            dialogService.closeDialog(
                questDialog = quest,
                author = author,
                currentUserActualizedInfo = userActualizedInfo,
                isByQuestionAuthor = isByQuestionAuthor,
                closeDialogMessages = CloseDialogMessages(),
                finishTime = messageTime,
                responder = responder,
            )
        }
    }

    private fun createKeyboard(vararg ECallbackData: ECallbackData): InlineKeyboardMarkup {
        val keyboard =
            listOf(*ECallbackData).map { button ->
                InlineKeyboardButton().also {
                    it.text = button.metaText!!
                    it.callbackData = button.id?.toString()
                }
            }.map { listOf(it) }
        return createKeyboard(keyboard)
    }

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) = InlineKeyboardMarkup().also { it.keyboard = keyboard }


    private data class Params(
        var userActualizedInfo: UserActualizedInfo,
        val update: Update,
    )
}