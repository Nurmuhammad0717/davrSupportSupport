package uz.davrmobile.support.exception

import uz.davrmobile.support.enm.ErrorCode

class ChatIsOpenException : DavrMobileException() {

    override fun errorCode(): ErrorCode =
        ErrorCode.CHAT_IS_OPEN
}
