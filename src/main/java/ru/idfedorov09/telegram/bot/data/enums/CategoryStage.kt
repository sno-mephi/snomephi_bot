package ru.idfedorov09.telegram.bot.data.enums

enum class CategoryStage {
    /**Фетчер категорий не активен**/
    WAITING,
    /**Отправлена inline клавиатура выбора действия с категорией**/
    ACTION_CHOOSING,
    /**Отправлена inline клавиатура создания категории**/
    ADDING,
    /**Отправлена inline клавиатура удаления категории**/
    DELETING,
    /**Отправлена inline клавиатура изменения категории**/
    EDITING,
}