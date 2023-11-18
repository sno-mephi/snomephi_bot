package ru.idfedorov09.telegram.bot.util

fun String?.isValidFullName() = this?.isNotEmpty()?: false

fun String?.isValidGroup() = this?.isNotEmpty()?: false