package uz.davrmobile.support.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.kafka.UserIdDto
import uz.davrmobile.support.usecase.ClearOldUserDataAfterResetPasswordUseCase


@Component
class Binders(
    private val clearOldUserDataAfterResetPasswordUseCase: ClearOldUserDataAfterResetPasswordUseCase
) {
    private val logger = KotlinLogging.logger {}

    val mapper = jacksonObjectMapper()

    @KafkaListener(topics = [KafkaTopics.CLEAR_USER_DATA_AFTER_RESET], groupId = "group-id-support")
    fun processClientData(payload: String) {
        logger.info { "CLEAR_USER_DATA_AFTER_RESET : $payload" }
        try {
            val request = mapper.readValue<UserIdDto>(payload)
            clearOldUserDataAfterResetPasswordUseCase.execute(request.userId)
        } catch (e: Exception) {
            logger.error { "CLEAR_USER_DATA_AFTER_RESET\nParse error: ${e.stackTraceToString()}" }
        }
    }
}
