package uz.davrmobile.support.usecase

import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.BaseResult
import uz.davrmobile.support.dto.RateModel
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.util.userId

@Component
class CloseChatUseCase(
    chatRepository: ChatRepository,
) : BaseChatUseCase(chatRepository) {

    fun execute(chatUid: String, rate: RateModel): BaseResult {
        if (!checkUser(userId(), chatUid)) {
            throw ChatNotFoundException()
        }

        chatRepository.evaluateChat(chatUid, rate.rate)
        chatRepository.closeChat(chatUid)
        return BaseResult()
    }
}
