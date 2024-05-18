package ru.idfedorov09.telegram.bot.fetchers.bot

import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.fetchers.DefaultFetcher
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.mephi.sno.libs.flow.belly.InjectData

@Component
class CreateExpContainerFetcher: DefaultFetcher() {

    @InjectData
    fun doFetch() = ExpContainer()
}