package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.annotation.FetcherPerms
import ru.idfedorov09.telegram.bot.data.enums.LastUserActionType
import ru.idfedorov09.telegram.bot.data.enums.TextCommands
import ru.idfedorov09.telegram.bot.data.enums.UserRole
import ru.idfedorov09.telegram.bot.data.model.CallbackData
import ru.idfedorov09.telegram.bot.data.model.MessageParams
import ru.idfedorov09.telegram.bot.data.model.UserActualizedInfo
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.repo.CallbackDataRepository
import ru.idfedorov09.telegram.bot.repo.UserRepository
import ru.idfedorov09.telegram.bot.service.MessageSenderService
import ru.idfedorov09.telegram.bot.service.SwitchKeyboardService
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher
import kotlin.jvm.optionals.getOrNull

/**
 * Фетчер для управления правами
 */
@Component
class PermissionsFetcher(
    private val callbackDataRepository: CallbackDataRepository,
    private val messageSenderService: MessageSenderService,
    private val userRepository: UserRepository,
    private val switchKeyboardService: SwitchKeyboardService,
) : DefaultFetcher() {
    @InjectData
    @FetcherPerms(UserRole.ROOT)
    fun doFetch(
        userActualizedInfo: UserActualizedInfo,
        update: Update,
    ): UserActualizedInfo {
        if (UserRole.ROOT !in userActualizedInfo.roles) return userActualizedInfo

        val params = Params(userActualizedInfo, update)
        return when {
            update.hasMessage() && update.message.hasText() -> textCommandsHandler(params)
            update.hasCallbackQuery() -> callbackQueryHandler(update, params)
            else -> userActualizedInfo
        }
    }

    private fun textCommandsHandler(params: Params): UserActualizedInfo {
        val text = params.update.message.text

        return text.run {
            when {
                startsWith(TextCommands.PERMISSIONS_SETUP()) -> entryUserTui(params)
                else -> commonTextHandler(params)
            }
        }
    }

    private fun commonTextHandler(params: Params): UserActualizedInfo {
        return when (params.userActualizedInfo.lastUserActionType) {
            LastUserActionType.PERMS_ENTER_TUI -> handleTui(params)
            else -> params.userActualizedInfo
        }
    }

    private fun callbackQueryHandler(
        update: Update,
        params: Params,
    ): UserActualizedInfo {
        val callbackId = update.callbackQuery.data?.toLongOrNull()
        callbackId ?: return params.userActualizedInfo
        val callbackData = callbackDataRepository.findById(callbackId).getOrNull() ?: return params.userActualizedInfo

        return callbackData.callbackData?.run {
            when {
                startsWith("#perms_cancel") -> permCancel(params)
                startsWith("#perm_add_role") -> addRoleAction(params, this)
//                startsWith("#perm_remove_role") -> roleControlMessage(params, true) TODO!
                startsWith("#perms_perm_add_") -> addRoleToUser(params, this)
                else -> params.userActualizedInfo
            }
        } ?: params.userActualizedInfo
    }

    private fun permCancel(params: Params): UserActualizedInfo {
        params.apply {
            messageSenderService.deleteMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    messageId = update.callbackQuery.message.messageId,
                ),
            )
            return userActualizedInfo.copy(
                lastUserActionType = LastUserActionType.DEFAULT,
            )
        }
    }

    private fun addRoleToUser(
        params: Params,
        callbackData: String,
    ): UserActualizedInfo {
        params.apply {
            val role = UserRole.valueOf(callbackData.split("_").last())
            val userId = callbackData.split("_").dropLast(1).last().toLong()

            val user =
                userRepository.findActiveUsersById(userId)!!.let {
                    it.roles.add(role)
                    userRepository.save(it)
                }
            val text = "Роль ${role.name} успешно добавлена пользователю ${user.fullName}!"

            switchKeyboardService.reshowKeyboard(userId)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = text,
                ),
            )
            return userActualizedInfo
        }
    }

    private fun addRoleAction(
        params: Params,
        callbackData: String,
    ): UserActualizedInfo {
        params.apply {
            val userId =
                callbackData
                    .split("_")
                    .lastOrNull()
                    ?.toLongOrNull()
                    ?: return userActualizedInfo

            val user = userRepository.findActiveUsersById(userId)!!
            val roles = UserRole.entries - user.roles

            val text = "Выбери роль, которую хочешь назначить:"
            val buttons =
                roles.map {
                    CallbackData(
                        callbackData = "#perms_perm_add_${userId}_${it.name}",
                        metaText = it.name,
                    ).save()
                }.toTypedArray()

            val keyboard = createKeyboard(*buttons)

            messageSenderService.sendMessage(
                MessageParams(
                    chatId = userActualizedInfo.tui,
                    text = text,
                    replyMarkup = keyboard,
                ),
            )
            return userActualizedInfo
        }
    }

    private fun handleTui(params: Params): UserActualizedInfo {
        params.apply {
            val tui =
                update.message.text.toLongOrNull() ?: run {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = userActualizedInfo.tui,
                            text = "Некорректный tui. Повтори попытку",
                        ),
                    )
                    return userActualizedInfo
                }
            val user =
                userRepository.findByTui(tui.toString()) ?: run {
                    messageSenderService.sendMessage(
                        MessageParams(
                            chatId = userActualizedInfo.tui,
                            text = "Такого юзера нет, повтори попытку",
                        ),
                    )
                    return userActualizedInfo
                }

            val text = "Нашел пользователя ${user.fullName}.\n"
            return permissionsSetupMessage(params, user.id!!, text)
        }
    }

    private fun entryUserTui(params: Params): UserActualizedInfo {
        val text = "Следующим сообщением напиши мне Telegram User Id человека, с ролями которого собираешься рофлить"
        val cancel =
            CallbackData(
                callbackData = "#perms_cancel",
                metaText = "отмена",
            ).save()

        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = text,
                replyMarkup = createKeyboard(cancel),
            ),
        )

        return params.userActualizedInfo.copy(
            lastUserActionType = LastUserActionType.PERMS_ENTER_TUI,
        )
    }

    private fun permissionsSetupMessage(
        params: Params,
        userId: Long,
        prefix: String,
    ): UserActualizedInfo {
        val text = "$prefix\nВыбери дальнейшее действие:"
        val addRole =
            CallbackData(
                callbackData = "#perm_add_role_$userId",
                metaText = "Выдать роль",
            ).save()

        // TODO!!!
        val removeRole =
            CallbackData(
                callbackData = "#perm_remove_role_$userId",
                metaText = "Выдать роль (не работает)",
            ).save()

        // TODO: кнопка отмены?

        val keyboard = createKeyboard(addRole, removeRole)
        messageSenderService.sendMessage(
            MessageParams(
                chatId = params.userActualizedInfo.tui,
                text = text,
                replyMarkup = keyboard,
            ),
        )

        return params.userActualizedInfo.copy(
            lastUserActionType = LastUserActionType.DEFAULT,
        )
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
