package uz.davrmobile.support.exception

import uz.davrmobile.support.enm.ErrorCode

class ChatIsNotOpenException : DavrMobileException() {

    override fun errorCode(): ErrorCode =
        ErrorCode.CHAT_IS_NOT_OPEN
}
