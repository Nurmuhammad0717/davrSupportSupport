package uz.davrmobile.support.exception

import uz.davrmobile.support.enm.ErrorCode

class ObjectNotFoundException(private val objName: String, private val identifier: Any) : DavrMobileException() {
    override fun errorCode(): ErrorCode = ErrorCode.NOT_FOUND_EXCEPTION
    override fun getErrorMessageArguments(): Array<Any> = arrayOf(objName, identifier)
}
