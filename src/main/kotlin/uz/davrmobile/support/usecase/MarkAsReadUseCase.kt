package uz.davrmobile.support.usecase

import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.BaseResult
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.enm.UserRole
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.getRoles
import uz.davrmobile.support.util.userId

@Component
class MarkAsReadUseCase(
    chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
) : BaseChatUseCase(chatRepository) {

    fun execute(chatUid: String): BaseResult {
        if (!checkUser(userId(), chatUid)) {
            throw ChatNotFoundException()
        }

        val senderType = getSenderType()
        messageRepository.markAsRead(chatUid, senderType)
        return BaseResult()
    }

    private fun getSenderType(): SenderType {
        val roles = getRoles()
        return if (roles.contains(UserRole.USER) && roles.size == 1) {
            SenderType.SUPPORT
        } else {
            SenderType.CLIENT
        }
    }
}
