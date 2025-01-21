package uz.davrmobile.support.usecase

import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.MessageModel
import uz.davrmobile.support.entity.Message
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.userId

@Component
class GetChatMessagesUseCase(
    chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
) : BaseChatUseCase(chatRepository) {

    private val log = KotlinLogging.logger { }

    fun execute(chatUid: String, page: Pageable): Page<MessageModel> {
        if (!checkUser(userId(), chatUid)) {
            log.info { userId() }
            throw ChatNotFoundException()
        }
        val chat = chatRepository.findByChatUID(chatUid)
            .orElseThrow(::ChatNotFoundException)

        return messageRepository.findAllByChatOrderByCreatedDateDesc(chat, page)
            .map(Message::toMessageModel)
    }
}
