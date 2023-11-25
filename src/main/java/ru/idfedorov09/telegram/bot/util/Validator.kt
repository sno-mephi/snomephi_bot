package ru.idfedorov09.telegram.bot.util

fun String?.isValidFullName() = this?.isNotEmpty() ?: false

fun String?.isValidGroup() = this?.let {
    it.isNotEmpty() && "([МСБмсб]{1})([0-9]{2})-([0-9]{3})".toRegex().matches(it)
} ?: false
