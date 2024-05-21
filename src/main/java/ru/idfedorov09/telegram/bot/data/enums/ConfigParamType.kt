package ru.idfedorov09.telegram.bot.data.enums

enum class ConfigParamType(
    val description: String,
) {
    INPUT("Текстовый формат (формат ввода)"),
    SELECT_ONE("Выбор одного из нескольких вариантов"),
    SELECT_MANY("Выбор нескольких из нескольких вариантов"),
}