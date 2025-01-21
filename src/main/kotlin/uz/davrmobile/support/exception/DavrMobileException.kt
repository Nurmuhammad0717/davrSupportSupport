package uz.davrmobile.support.exception

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import uz.davrmobile.support.dto.BaseErrorMessage
import uz.davrmobile.support.enm.ErrorCode

sealed class DavrMobileException : RuntimeException() {

    abstract fun errorCode(): ErrorCode

    open fun getErrorMessageArguments(): Array<Any>? = null

    @Suppress("TooGenericExceptionCaught")
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseErrorMessage {
        val errorMessage = try {
            errorMessageSource.getMessage(errorCode().name, getErrorMessageArguments(), LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message
        }
        return BaseErrorMessage(errorCode().code, errorMessage)
    }
}
