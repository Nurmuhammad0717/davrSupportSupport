package uz.davrmobile.support.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uz.davrmobile.support.entity.Chat
import java.util.*
import javax.transaction.Transactional

@Repository
@Suppress("MaximumLineLength")
interface ChatRepository : BaseRepository<Chat> {

    fun findByModeratorIdAndChatUID(moderatorId: Long, chatUID: String): Optional<Chat>

    fun findByClientIdAndChatUID(clientId: Long, chatUID: String): Optional<Chat>

    fun existsByClientIdAndOpenTrue(clientId: Long): Boolean

    fun findByChatUID(uid: String): Optional<Chat>

    @Modifying
    @Transactional
    @Query(
        "update chat set moderator_id = :moderatorId, moderator_username = :phone, moder_uid = :moderUuid, moderator_name = :moderatorName  where chatuid = :id",
        nativeQuery = true
    )
    fun setModerator(moderatorId: Long, id: String, phone: String, moderUuid: String?, moderatorName: String?)

    @Modifying
    @Transactional
    @Query("update chat set open = false where chatUID = :chatUID", nativeQuery = true)
    fun closeChat(chatUID: String)

    @Modifying
    @Transactional
    @Query("update chat set rate = :rate where chatUID = :chatUID", nativeQuery = true)
    fun evaluateChat(chatUID: String, rate: Short)

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.open = false, c.clientId = :negatedClientId WHERE c.clientId = :clientId")
    fun updateChatByClientId(clientId: Long, negatedClientId: Long): Int
}
