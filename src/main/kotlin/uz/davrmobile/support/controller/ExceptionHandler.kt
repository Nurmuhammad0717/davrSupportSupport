package uz.davrmobile.support.controller

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import uz.davrmobile.support.dto.BaseErrorMessage
import uz.davrmobile.support.enm.ErrorCode
import uz.davrmobile.support.exception.BotErrorModel
import uz.davrmobile.support.exception.DavrMobileException
import uz.davrmobile.support.exception.DeveloperLoggerBot
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import mu.KotlinLogging
import uz.davrmobile.support.bot.backend.BaseMessage
import uz.davrmobile.support.bot.backend.DBusinessException

@ControllerAdvice
class ExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource,
    private val loggerBotService: DeveloperLoggerBot,
) {

    private final val logger = KotlinLogging.logger { }

    @ExceptionHandler(DBusinessException::class)
    fun handleAccountException(exception: DBusinessException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
    @ExceptionHandler(DavrMobileException::class)
    fun handleDavrMobileException(exception: DavrMobileException): ResponseEntity<BaseErrorMessage> =
        ResponseEntity.badRequest()
            .body(exception.getErrorMessage(errorMessageSource))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleSupportValidationException(exception: MethodArgumentNotValidException): ResponseEntity<BaseErrorMessage> =
        ResponseEntity.badRequest()
            .body(BaseErrorMessage(HttpStatus.BAD_REQUEST.value(), exception.localizedMessage))

    @ExceptionHandler(Throwable::class)
    @Suppress("ReturnCount")
    fun handleOtherExceptions(throwable: Throwable): ResponseEntity<BaseErrorMessage> {
        if (throwable is DavrMobileException) {
            return ResponseEntity.badRequest()
                .body(throwable.getErrorMessage(errorMessageSource))
        }
        if (throwable is AccessDeniedException) {
            return ResponseEntity.badRequest()
                .body(BaseErrorMessage(HttpStatus.FORBIDDEN.value(), throwable.localizedMessage))
        }

        logger.error { throwable.stackTraceToString() }
        val dto = throwable.toBotErrorModel()
        loggerBotService.sendError(dto)

        return ResponseEntity.badRequest()
            .body(
                BaseErrorMessage(
                    ErrorCode.INTERNAL_SERVER_ERROR.code,
                    "Internal server error on support-service\n${throwable.message}"
                )
            )
    }

    private fun Throwable.toBotErrorModel(): BotErrorModel {
        val writer = StringWriter()
        this.printStackTrace(PrintWriter(writer))
        writer.close()
        return BotErrorModel(Date().time, "support", writer.toString())
    }
}
