package uz.davrmobile.support.exception

import uz.davrmobile.support.enm.ErrorCode

class ChatNotFoundException : DavrMobileException() {

    override fun errorCode(): ErrorCode =
        ErrorCode.CHAT_NOT_FOUND
}
