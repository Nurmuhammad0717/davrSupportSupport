package uz.davrmobile.support.usecase

import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.BaseResult
import uz.davrmobile.support.exception.ChatNotFoundException
import uz.davrmobile.support.feign.client.AuthService
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.util.phoneNumber
import uz.davrmobile.support.util.userId

@Component
class BindChatToMeUseCase(
    chatRepository: ChatRepository,
    private val authService: AuthService,
) : BaseChatUseCase(chatRepository) {

    fun execute(chatUid: String): BaseResult {
        chatRepository.findByChatUID(chatUid)
            .orElseThrow(::ChatNotFoundException)

        val userId = userId()
        val user = authService.getUserInfo()

        chatRepository.setModerator(
            moderatorId = userId,
            id = chatUid,
            phone = getUsername(phoneNumber()),
            moderUuid = user.hashId,
            moderatorName = user.fullName,
        )
        return BaseResult()
    }
}
