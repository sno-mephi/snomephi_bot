package ru.idfedorov09.telegram.bot.fetchers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер который позволяет легко создавать консоли и управлять ими
 * T - тип используемого класса для передачи параметров
 */
@Component
open class ConsoleFetcher<T> : DefaultFetcher() {

    private val currentFetcherName = this::class.qualifiedName
    private val callbacksList: MutableList<ButtonActionBridge<T>> = mutableListOf()

    @Autowired
    private lateinit var callbackDataRepository: CallbackDataRepository

    fun createButton(
        callbackData: String? = null,
        text: String,
        url: String? = null,
        onClickAction: (T) -> Unit = {}
    ) = CallbackData(callbackData = callbackData, metaText = text, metaUrl = url).save().also {
        callbacksList.add(ButtonActionBridge(it, onClickAction))
    }

    fun CallbackData.withCallbackData(callbackData: String) = this.copy(
        callbackData = callbackData
    ).save()

    fun CallbackData.withText(text: String) = this.copy(
        metaText = text
    ).save()

    fun CallbackData.withUrl(url: String) = this.copy(
        metaUrl = url
    ).save()

    fun callbackQueryHandler(params: T) {
        val callbackId = flowContext.get<Update>()?.callbackQuery?.data?.toLongOrNull() ?: return
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return
        callbackData.callbackData ?: return

        callbackData.callbackData.let { receivedCallback ->
            callbacksList
                .filter { it.callbackData.callbackData != null }
                .forEach {
                    if (receivedCallback.startsWith(it.callbackData.callbackData!!)) it.onClickAction(params)
                }
        }
    }

    private fun createButtons(vararg buttons: CallbackData) = createKeyboard(
        buttons.map { button ->
            InlineKeyboardButton().also {
                it.text = button.metaText!!
                it.callbackData = button.id?.toString()
                it.url = button.metaUrl
            }
        }.map { listOf(it) }
    )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }

    private fun CallbackData.save() = callbackDataRepository.save(this)

    data class ButtonActionBridge<T> (
        val callbackData: CallbackData,
        val onClickAction: (T) -> Unit
    )
}