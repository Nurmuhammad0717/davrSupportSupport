package uz.davrmobile.support.usecase

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.BaseResult
import uz.davrmobile.support.enm.MessageType
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.entity.Message
import uz.davrmobile.support.exception.ChatIsNotOpenException
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.userId
import java.util.*

@Component
class AskToCloseChatUseCase(
    chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val template: SimpMessagingTemplate,
) : BaseChatUseCase(chatRepository) {

    fun execute(chatUid: String): BaseResult {
        val userId = userId()
        if (!checkUser(userId, chatUid)) {
            throw ChatNotFoundException()
        }
        val chat = chatRepository.findByChatUID(chatUid)
            .orElseThrow { ChatNotFoundException() }
        if (!chat.open) {
            throw ChatIsNotOpenException()
        }

        val message = Message(
            text = ResourceBundle.getBundle("message")
                .getString("askForCloseMessage"),
            senderId = userId,
            messageType = MessageType.TEXT,
            senderType = SenderType.ASK_CLOSE,
            chat = chat,
        )
        messageRepository.save(message)
        template.convertAndSend("/queue/${chat.clientUid}/message", message)
        return BaseResult()
    }
}
