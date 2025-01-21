package uz.davrmobile.support.usecase

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uz.davrmobile.support.repository.ChatRepository

@Service
class ClearOldUserDataAfterResetPasswordUseCase(
    private val chatRepository: ChatRepository,
) {
    private val logger = KotlinLogging.logger {}

    fun execute(userId: Long) {
        val updatedRows = chatRepository.updateChatByClientId(clientId = userId, negatedClientId = -userId)
        logger.info { "After reset $updatedRows rows changed on the chats table!" }
    }
}