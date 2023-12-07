package ru.idfedorov09.telegram.bot.data.keyboards

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.CallbackCommands
import ru.idfedorov09.telegram.bot.repo.CategoryRepository

class CategoryKeyboards {
    companion object{
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
                )
            )
        }
        fun choosingCategory(page: Long, pageSize: Long, catRep: CategoryRepository): InlineKeyboardMarkup{
            val body=choosingCategoryBody(page,pageSize,catRep)
            val nav=choosingCategoryNav(page,pageSize,catRep)
            body.addAll(nav)
            return InlineKeyboardMarkup(body)
        }
        fun choosingCategoryBody(page: Long, pageSize: Long, catRep: CategoryRepository): MutableList<MutableList<InlineKeyboardButton>>{
            val categoriesList = catRep.findCategoriesByPage(page,pageSize)
            val keyboard = mutableListOf(mutableListOf<InlineKeyboardButton>())
            for(i in categoriesList.indices){
                keyboard.add(
                    mutableListOf(
                        InlineKeyboardButton("${categoriesList[i].title} [${categoriesList[i].suffix}]").also{
                            it.callbackData = CallbackCommands.CATEGORY_CHOOSE.format(categoriesList[i].id)
                        }
                    )
                )
            }
            for(i in categoriesList.size until pageSize){
                keyboard.add(
                    mutableListOf(
                        InlineKeyboardButton("----").also{
                            it.callbackData = CallbackCommands.VOID.data
                        }
                    )
                )
            }
            return keyboard
        }
        fun choosingCategoryNav(page: Long, pageSize: Long, catRep: CategoryRepository): MutableList<MutableList<InlineKeyboardButton>>{
            val pageCount=catRep.count()/pageSize+1
            return mutableListOf(
                mutableListOf(
                    InlineKeyboardButton("⬅️ Назад").also {
                        it.callbackData = CallbackCommands.CATEGORY_PAGE.format((page-1).mod(pageCount))
                    },
                    InlineKeyboardButton("${page+1}/$pageCount").also {
                        it.callbackData = CallbackCommands.VOID.data
                    },
                    InlineKeyboardButton("Вперёд ➡️").also {
                        it.callbackData = CallbackCommands.CATEGORY_PAGE.format((page+1).mod(pageCount))
                    },
                ),
                mutableListOf(
                    InlineKeyboardButton("В меню ↩️").also {
                        it.callbackData = CallbackCommands.CATEGORY_ACTION_MENU.data
                    },
                )
            )
        }
    }
}