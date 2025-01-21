package uz.davrmobile.support.usecase

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import uz.davrmobile.support.enm.MessageType
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.entity.Chat
import uz.davrmobile.support.entity.Message
import uz.davrmobile.support.exception.ChatIsOpenException
import uz.davrmobile.support.feign.client.AuthService
import uz.davrmobile.support.model.OpenChatRequest
import uz.davrmobile.support.model.OpenChatResponse
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.phoneNumberAsUsername
import uz.davrmobile.support.util.userId
import java.util.ResourceBundle
import java.util.UUID

@Component
class OpenChatUseCase(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val authService: AuthService,
) {

    companion object {
        private const val DEFAULT_SENDER_ID = -1L
    }

    fun execute(request: OpenChatRequest): OpenChatResponse {
        val userId = userId()
        if (chatRepository.existsByClientIdAndOpenTrue(userId)) {
            throw ChatIsOpenException()
        }

        val chat = createChat(request, userId)
        saveFirstMessage(chat)
        return OpenChatResponse(chat.chatUID)
    }

    private fun createChat(request: OpenChatRequest, userId: Long): Chat =
        chatRepository.save(
            Chat(
                appealSection = request.section,
                chatUID = UUID.randomUUID()
                    .toString()
                    .replace("-", ""),
                clientId = userId,
                clientUsername = phoneNumberAsUsername(),
                clientUid = authService.getUserInfo().hashId
            )
        )

    private fun saveFirstMessage(chat: Chat) {
        val text = ResourceBundle.getBundle("message", LocaleContextHolder.getLocale())
            .getString("commonMessage")
        messageRepository.save(
            Message(
                text = text,
                senderId = DEFAULT_SENDER_ID,
                messageType = MessageType.TEXT,
                senderType = SenderType.SUPPORT,
                chat = chat,
                fileId = null,
                read = false,
            )
        )
    }
}
