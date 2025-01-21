package uz.davrmobile.support.usecase

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.SupportResultQueryDto
import uz.davrmobile.support.enm.UserRole
import uz.davrmobile.support.repository.CustomChatRepository
import uz.davrmobile.support.util.getRoles
import uz.davrmobile.support.util.userId

@Component
class GetChatsUseCase(
    private val customChatRepository: CustomChatRepository,
) {

    fun execute(pageable: Pageable): Page<SupportResultQueryDto> {
        val userId = userId()
        val roles = SecurityContextHolder.getContext()
            .getRoles()

        return if (roles.contains(UserRole.USER) && roles.size == 1) {
            val count = customChatRepository.countUsersChats(userId)
            val sqlResponse = customChatRepository.findPagedMessagesByUserId(userId, pageable)
            PageImpl(sqlResponse, pageable, count)
        } else if (roles.contains(UserRole.CALL_CENTER)) {
            val chatList = customChatRepository.getOpenChatListForSupporter(userId)
            val start = pageable.offset.toInt()
            val end = (start + pageable.pageSize).coerceAtMost(chatList.size)
            PageImpl(chatList.subList(start, end), pageable, chatList.size.toLong())
        } else {
            val chatList = customChatRepository.getAdminChatList(pageable)
            val count = chatList.size.toLong()
            PageImpl(chatList, pageable, count)
        }
    }
}