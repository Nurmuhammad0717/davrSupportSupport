package uz.davrmobile.support.bot.backend

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource

sealed class DBusinessException : RuntimeException() {

    abstract fun errorCode(): ErrorCode

    open fun getErrorMessageArguments(): Array<Any?>? = null

    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        val errorMessage = try {
            errorMessageSource.getMessage(errorCode().name, getErrorMessageArguments(), LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message
        }
        return BaseMessage(errorCode().code, errorMessage)
    }
}


class UserNotFoundException : DBusinessException() {
    override fun errorCode() = ErrorCode.USER_NOT_FOUND
}

class BadCredentialsException : DBusinessException() {
    override fun errorCode() = ErrorCode.BAD_CREDENTIALS
}

class AccessDeniedException : DBusinessException() {
    override fun errorCode() = ErrorCode.ACCESS_DENIED
}

class UsernameAlreadyExists : DBusinessException() {
    override fun errorCode() = ErrorCode.USERNAME_ALREADY_EXISTS
}

class UserAlreadyExistException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.USER_ALREADY_EXISTS
}

class UnSupportedMessageTypeException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.UN_SUPPORTED_MESSAGE_TYPE
}

class NoSessionInQueue : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.NO_SESSION_IN_QUEUE
}

class SomethingWentWrongException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.SOMETHING_WENT_WRONG
}

class SessionNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_NOT_FOUND
}

class SessionAlreadyBusyException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_ALREADY_BUSY
}

class SessionClosedException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_CLOSED
}

class MessageNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.MESSAGE_NOT_FOUND
}

class BotNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_NOT_FOUND
}

class BusySessionException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.BUSY_SESSION
}

class FileNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.FILE_NOT_FOUND
}

class OperatorLanguageNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.OPERATOR_LANGUAGE_NOT_FOUND
}

class StandardAnswerNotFoundException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.STANDARD_ANSWER_NOT_FOUND
}

class StandardAnswerAlreadyExistsException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.STANDARD_ANSWER_ALREADY_EXISTS
}

class BotAlreadyStoppedException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_ALREADY_STOPPED
}

class BotAlreadyActiveException : DBusinessException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_ALREADY_ACTIVE
}