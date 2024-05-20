package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.SurveyAnswer
import ru.idfedorov09.telegram.bot.data.model.SurveyQuestion

interface SurveyAnswerRepository : JpaRepository<SurveyAnswer, Long>