package ru.idfedorov09.telegram.bot.util

fun String?.isValidFullName() = this?.let {
    it.isNotEmpty() && it.length < 128
} ?: false

fun String?.isValidGroup() = this?.let {
    it.isNotEmpty() && "([АМСБамсб]{1})([0-9]{2})-([0-9]{3})".toRegex().matches(it)
} ?: false
