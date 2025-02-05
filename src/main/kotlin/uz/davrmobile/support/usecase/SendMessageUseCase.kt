package uz.davrmobile.support.usecase

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.MessageDto
import uz.davrmobile.support.dto.MessageModel
import uz.davrmobile.support.enm.MessageType
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.enm.UserRole
import uz.davrmobile.support.entity.Message
import uz.davrmobile.support.exception.ChatIsNotOpenException
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.getRoles
import uz.davrmobile.support.util.roles
import uz.davrmobile.support.util.userId

@Component
class SendMessageUseCase(
    chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val template: SimpMessagingTemplate,
) : BaseChatUseCase(chatRepository) {

    companion object {
        private const val MIN_FILE_SIZE = 3
    }

    fun execute(chatUid: String, dto: MessageDto): MessageModel {
        val roles = roles()
        val message = if (roles.contains(UserRole.USER) && roles.size == 1) {
            sendMessageAsUser(chatUid, dto)
        } else {
            sendMessageAsSupport(chatUid, dto)
        }
        return message.toMessageModel()
    }

    private fun sendMessageAsUser(chatUid: String, dto: MessageDto): Message {
        val userId = userId()

        val chat = chatRepository.findByClientIdAndChatUID(userId, chatUid)
            .orElseThrow { ChatNotFoundException() }

        if (!chat.open) {
            throw ChatIsNotOpenException()
        }

        val messageType = if ((dto.fileId?.length ?: 0) < MIN_FILE_SIZE) {
            MessageType.TEXT
        } else {
            MessageType.FILE
        }

        val message = Message(
            text = dto.text,
            senderId = userId,
            messageType = messageType,
            senderType = SenderType.CLIENT,
            chat = chat,
            fileId = dto.fileId,
            fileName = dto.fileName,
            fileSize = dto.fileSize,
        )

        val savedMessage = messageRepository.save(message)
        template.convertAndSend("/queue/${chat.moderUid}/message", savedMessage)
        return savedMessage
    }

    private fun sendMessageAsSupport(chatUid: String, dto: MessageDto): Message {
        val userId = userId()

        if (!checkUser(userId, chatUid)) {
            throw ChatNotFoundException()
        }
        val chat = chatRepository.findByChatUID(chatUid).get()
        if (!chat.open) {
            throw ChatIsNotOpenException()
        }

        val messageType = if (dto.fileId.isNullOrBlank()) {
            MessageType.TEXT
        } else {
            MessageType.FILE
        }

        val message = Message(
            text = dto.text,
            senderId = userId,
            messageType = messageType,
            senderType = SenderType.SUPPORT,
            chat = chat,
            fileId = dto.fileId,
            fileName = dto.fileName,
            fileSize = dto.fileSize,
        )

        val savedMessage = messageRepository.save(message)
        template.convertAndSend("/queue/${chat.clientUid}/message", savedMessage)
        return savedMessage
    }
}
