package ru.idfedorov09.telegram.bot.data.keyboards

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.repo.CategoryRepository

object CategoryKeyboards {
    fun choosingAction(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("✏️ Изменить").also {
                        it.callbackData = CallbackCommands.CATEGORY_EDIT.data
                    },
                    InlineKeyboardButton("✅ Добавить").also {
                        it.callbackData = CallbackCommands.CATEGORY_ADD.data
                    },
                    InlineKeyboardButton("❌ Удалить").also {
                        it.callbackData = CallbackCommands.CATEGORY_DELETE.data
                    },
                ),
                mutableListOf(
                    InlineKeyboardButton("В меню ↩️").also {
                        it.callbackData = CallbackCommands.CATEGORY_EXIT.data
                    },
                ),
            ),
        )
    }

    fun choosingCategory(
        page: Long,
        pageSize: Long,
        catRep: CategoryRepository,
    ): InlineKeyboardMarkup {
        val body = choosingCategoryBody(page, pageSize, catRep)
        val nav = choosingCategoryNav(page, pageSize, catRep)
        body.addAll(nav)
        return InlineKeyboardMarkup(body)
    }

    fun choosingCategoryBody(
        page: Long,
        pageSize: Long,
        catRep: CategoryRepository,
    ): MutableList<MutableList<InlineKeyboardButton>> {
        val categoriesList = catRep.findCategoriesByPage(page, pageSize)
        val keyboard = mutableListOf(mutableListOf<InlineKeyboardButton>())
        for (i in categoriesList.indices) {
            keyboard.add(
                mutableListOf(
                    InlineKeyboardButton("${categoriesList[i].title} #${categoriesList[i].suffix}").also {
                        it.callbackData = CallbackCommands.CATEGORY_CHOOSE.format(categoriesList[i].id, page)
                    },
                ),
            )
        }
        for (i in categoriesList.size until pageSize) {
            keyboard.add(
                mutableListOf(
                    InlineKeyboardButton("----").also {
                        it.callbackData = CallbackCommands.VOID.data
                    },
                ),
            )
        }
        return keyboard
    }

    fun choosingCategoryNav(
        page: Long,
        pageSize: Long,
        catRep: CategoryRepository,
    ): MutableList<MutableList<InlineKeyboardButton>> {
        val catCount = catRep.count()
        val pageCount =
            if (catCount % pageSize != 0L || catCount == 0L) {
                catCount / pageSize + 1
            } else {
                catCount / pageSize
            }
        return mutableListOf(
            mutableListOf(
                InlineKeyboardButton("⬅️ Назад").also {
                    it.callbackData =
                        if (pageCount != 1L) {
                            CallbackCommands.CATEGORY_PAGE.format((page - 1).mod(pageCount))
                        } else {
                            CallbackCommands.VOID.data
                        }
                },
                InlineKeyboardButton("${page + 1}/$pageCount").also {
                    it.callbackData = CallbackCommands.VOID.data
                },
                InlineKeyboardButton("Вперёд ➡️").also {
                    it.callbackData =
                        if (pageCount != 1L) {
                            CallbackCommands.CATEGORY_PAGE.format((page + 1).mod(pageCount))
                        } else {
                            CallbackCommands.VOID.data
                        }
                },
            ),
            mutableListOf(
                InlineKeyboardButton("В меню ↩️").also {
                    it.callbackData = CallbackCommands.CATEGORY_ACTION_MENU.format(0)
                },
            ),
        )
    }

    fun confirmationAction(
        catId: Long,
        prevPage: Long,
    ): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("✅ Да").also {
                        it.callbackData = CallbackCommands.CATEGORY_CONFIRM.format(catId)
                    },
                    InlineKeyboardButton("Нет ❌").also {
                        it.callbackData = CallbackCommands.CATEGORY_CHOOSE_MENU.format(prevPage)
                    },
                ),
            ),
        )
    }

    fun inputCancel(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("❌ Отмена").also {
                        it.callbackData = CallbackCommands.CATEGORY_INPUT_CANCEL.data
                    },
                ),
            ),
        )
    }

    fun confirmationDone(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("В меню ↩️").also {
                        it.callbackData = CallbackCommands.CATEGORY_ACTION_MENU.format(1)
                    },
                ),
            ),
        )
    }

    fun questionIsUnremovable(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("✅ Да").also {
                        it.callbackData = CallbackCommands.CATEGORY_IS_UNREMOVABLE.format(1)
                    },
                    InlineKeyboardButton("❌ Нет").also {
                        it.callbackData = CallbackCommands.CATEGORY_IS_UNREMOVABLE.format(0)
                    },
                ),
                mutableListOf(
                    InlineKeyboardButton("❌ Отмена").also {
                        it.callbackData = CallbackCommands.CATEGORY_INPUT_CANCEL.data
                    },
                ),
            ),
        )
    }
}
