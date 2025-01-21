package uz.davrmobile.support.exception

import uz.davrmobile.support.enm.ErrorCode

class IllegalOperationEx : DavrMobileException() {

    override fun errorCode(): ErrorCode =
        ErrorCode.ILLEGAL_OPERATION
}
