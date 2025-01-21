package uz.davrmobile.support.repository

import javax.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uz.davrmobile.support.dto.SupportResultQueryDto
import uz.davrmobile.support.enm.SenderType
import uz.davrmobile.support.entity.Chat
import uz.davrmobile.support.entity.Message

@Suppress("MaximumLineLength")
interface MessageRepository : JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    fun findAllByChatOrderByCreatedDateDesc(chat: Chat, pageable: Pageable): Page<Message>

//    @Query(
//        """SELECT ch.appeal_section as
//                                      appealSection, ch.open as open, ch.id as id, ch.client_username as clientUsername, ch.chatuid as chatUid,
//                       ch.client_id as clientId, m2.message_type as type, m2.file_id as fileId, m2.text as text, m2.created_date as
//                                      createdDate, m2.file_name as fileName, m2.file_size as fileSize, msg.unread as unread FROM (select m.chat_id, max(m.id) as id, max(m.created_date), count(m.read) filter ( where m.read = false and m.sender_type='CLIENT') as unread
//                                                        from message m group by m.chat_id) msg
//                                          left join chat ch on ch.id = msg.chat_id
//                left join message m2 on msg.id = m2.id where ch.moderator_id =:moderatorId and ch.open = true order by m2.created_date desc""",
//        nativeQuery = true
//    )
//    fun getOpenChatListForSupporter(moderatorId: Long): List<SupportResultQueryDto>

    @Query(
        """SELECT ch.appeal_section as
                                      appealSection, ch.open as open, ch.id as id, ch.client_username as clientUsername, ch.chatuid as chatUid,
                       ch.client_id as clientId, m2.message_type as type, m2.file_id as fileId, m2.text as text, m2.created_date as
                                      createdDate, m2.file_name as fileName, m2.file_size as fileSize, msg.unread as unread FROM (select m.chat_id, max(m.id) as id, max(m.created_date), count(m.read) filter ( where m.read = false and m.sender_type='CLIENT') as unread
                                                        from message m group by m.chat_id) msg
                                          left join chat ch on ch.id = msg.chat_id
                left join message m2 on msg.id = m2.id order by m2.created_date desc""",
        nativeQuery = true
    )
    fun getAdminChatList(pageable: Pageable): Page<SupportResultQueryDto>

    @Modifying
    @Transactional
    @Query(
        "UPDATE Message m SET m.read = true WHERE m.chat.id IN (SELECT c.id FROM Chat c WHERE c.chatUID = :chatUid) AND m.senderType = :senderType"
    )
    fun markAsRead(@Param("chatUid") chatUid: String, @Param("senderType") senderType: SenderType)

    fun findByChatAndId(chat: Chat, id: Long): Message?
}
