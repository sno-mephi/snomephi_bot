package ru.idfedorov09.telegram.bot.service

import org.springframework.stereotype.Service

@Service
class KeyBoardService {
    fun changeKeyBoard(keyBoardType){

        when (keyBoardType){
            DEFAULT -> createDefaultKeyBoard()
        }
    }

    private fun createDefaultKeyBoard() {}

    private fun deleteDefaultKeyBoard() {}

    private fun createQuestKeyBoard() {}

    private fun deleteQuestKeyBoard() {}
}