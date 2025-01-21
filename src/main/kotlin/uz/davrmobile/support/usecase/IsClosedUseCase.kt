package uz.davrmobile.support.usecase

import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.BaseResult
import uz.davrmobile.support.dto.IsCloseDto
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.entity.Chat
import uz.davrmobile.support.exception.ChatIsNotOpenException
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.exception.IllegalOperationEx
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.repository.MessageRepository
import uz.davrmobile.support.util.userId

@Component
class IsClosedUseCase(
    chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
) : BaseChatUseCase(chatRepository) {

    fun execute(chatId: String, request: IsCloseDto): BaseResult {
        if (!checkUser(userId(), chatId)) {
            throw ChatNotFoundException()
        }
        val chat = chatRepository.findByChatUID(chatId)
            .orElseThrow { ChatNotFoundException() }
        if (!chat.open) {
            throw ChatIsNotOpenException()
        }

        updateMessage(chat, request)
        return BaseResult()
    }

    private fun updateMessage(chat: Chat, request: IsCloseDto) {
        messageRepository.findByChatAndId(chat, request.messageId)
            ?.let {
                when (it.senderType) {
                    SenderType.ASK_CLOSE -> {
                        messageRepository.save(
                            it.copy(
                                senderType = if (request.isClose) {
                                    SenderType.ASK_CLOSE_ACCEPTED
                                } else {
                                    SenderType.ASK_CLOSE_DENIED
                                },
                            ).apply { this.id = it.id }
                        )
                    }

                    else -> {
                        throw IllegalOperationEx()
                    }
                }
            }
    }
}
