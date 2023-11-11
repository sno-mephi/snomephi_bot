package ru.idfedorov09.telegram.bot.util

import com.google.gson.Gson
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.config.BotContainer
import ru.idfedorov09.telegram.bot.service.RedisService
import java.util.regex.Pattern

@Component
class UpdatesUtil(
    private val gson: Gson,
    private val redisService: RedisService,
    private val botContainer: BotContainer,
) {
    companion object {
        private val log = LoggerFactory.getLogger(UpdatesUtil::class.java)
    }

    fun getChatId(update: Update?): String? {
        return getByPattern(update, "\"chat\"\\s*:\\s*\\{\"id\"\\s*:\\s*(-?\\d+)")
    }

    fun getText(update: Update?): String? {
        var text = getByPattern(update, "\"text\"\\s*:\\s*\"(.+?)\"")
        if (text == null) text = getByPattern(update, "\"caption\"\\s*:\\s*\"(.+?)\"")
        return text
    }

    fun getByPattern(update: Update?, pattern: String): String? {
        val updateJson = gson.toJson(update)
        var result: String? = null
        val r = Pattern.compile(pattern)
        val matcher = r.matcher(updateJson)
        if (matcher.find()) {
            result = matcher.group(1)
        }
        return result
    }

    fun getChatKey(chatId: String): String {
        return "cht_num_$chatId"
    }

    private fun removeKeyPrefix(prefix: String) {
        val keys = redisService.keys("$prefix*")
        if (!keys.isNullOrEmpty()) {
            redisService.del(keys)
        }
    }

    @PostConstruct
    fun clearAllQues() {
        log.info("Removing old ques data..")
        removeKeyPrefix("cht_num_")
        removeKeyPrefix(botContainer.messageQueuePrefix)
        log.info("Removed.")
    }
}
