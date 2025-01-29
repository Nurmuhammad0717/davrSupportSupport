package uz.davrmobile.support.bot.backend

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource

sealed class SupportBotException : RuntimeException() {
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

class UserNotFoundException : SupportBotException() {
    override fun errorCode() = ErrorCode.USER_NOT_FOUND
}

class BadCredentialsException : SupportBotException() {
    override fun errorCode() = ErrorCode.BAD_CREDENTIALS
}

class UnSupportedMessageTypeException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.UN_SUPPORTED_MESSAGE_TYPE
}

class SomethingWentWrongException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.SOMETHING_WENT_WRONG
}

class SessionNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_NOT_FOUND
}

class SessionAlreadyBusyException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_ALREADY_BUSY
}

class SessionAlreadyClosedException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_CLOSED
}

class MessageNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.MESSAGE_NOT_FOUND
}

class BotNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_NOT_FOUND
}

class BusySessionException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.BUSY_SESSION
}

class FileNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.FILE_NOT_FOUND
}

class OperatorLanguageNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.OPERATOR_LANGUAGE_NOT_FOUND
}

class StandardAnswerNotFoundException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.STANDARD_ANSWER_NOT_FOUND
}

class StandardAnswerAlreadyExistsException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.STANDARD_ANSWER_ALREADY_EXISTS
}

class BotAlreadyStoppedException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_ALREADY_STOPPED
}

class BotAlreadyActiveException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_ALREADY_ACTIVE
}

class NoAuthorityException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.NO_AUTHORITY
}

class SessionNotConnectedToOperatorException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.SESSION_NOT_CONNECTED_TO_OPERATOR
}
class TextCantBeEmptyException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.TEXT_CANT_BE_EMPTY
}

class MaximumTextLengthException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.MAXIMUM_TEXT_LENGTH
}
class BotTokenNotValidException : SupportBotException() {
    override fun errorCode(): ErrorCode = ErrorCode.BOT_TOKEN_NOT_VALID
}