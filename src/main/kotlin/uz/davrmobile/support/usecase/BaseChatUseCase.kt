package uz.davrmobile.support.usecase

import mu.KotlinLogging
import uz.davrmobile.support.enm.UserRole
import uz.davrmobile.support.repository.ChatRepository
import uz.davrmobile.support.util.getRoles

open class BaseChatUseCase(
    protected val chatRepository: ChatRepository
) {
    private val logger = KotlinLogging.logger { }

    protected fun checkUser(userId: Long, chatUid: String): Boolean {
        val roles = getRoles()

        return if (roles.contains(UserRole.USER) && roles.size == 1) {
            chatRepository.findByClientIdAndChatUID(userId, chatUid).isPresent
        } else {
            chatRepository.findByModeratorIdAndChatUID(userId, chatUid).isPresent
        }
    }

    protected fun getUsername(phone: String): String =
        if (phone.contains(":")) {
            phone.split(":")[0]
        } else {
            phone
        }
}
