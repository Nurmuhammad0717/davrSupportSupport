package uz.davrmobile.support.repository

import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Date
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uz.davrmobile.support.dto.SupportResultQueryDto
import uz.davrmobile.support.enm.AppealSection
import uz.davrmobile.support.enm.MessageType

@Repository
class CustomChatRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun countUsersChats(userId: Long): Long {
        val sql = """
            SELECT count(ch.id)
            FROM (
                SELECT m.chat_id,
                       max(m.id) as id,
                       count(m.read) FILTER (WHERE m.read = false AND m.sender_type = 'SUPPORT') as unread
                FROM message m
                GROUP BY m.chat_id
            ) msg
            LEFT JOIN chat ch ON ch.id = msg.chat_id
            LEFT JOIN message m2 ON msg.id = m2.id
            WHERE ch.client_id = :userId
        """

        val paramMap = mapOf("userId" to userId)
        return namedParameterJdbcTemplate.queryForObject(sql, paramMap, Long::class.java)!!
    }

    fun findPagedMessagesByUserId(userId: Long, pageable: Pageable): List<SupportResultQueryDto> {
        val sql = """
        SELECT ch.appeal_section as appealSection,
                       ch.open           as open,
                       ch.id             as id,
                       ch.chatuid        as chatUid,
                       ch.moderator_name as moderatorName,
                       ch.client_id      as clientId,
                       m2.message_type   as type,
                       m2.file_id        as fileId,
                       m2.text           as text,
                       m2.created_date   as createdDate,
                       msg.unread        as unread,
                       ch.client_username as clientUsername,
                       m2.file_name      as fileName,
                       m2.file_size      as fileSize
                FROM (select m.chat_id,
                             max(m.id)                                                                  as id,
                             max(m.created_date),
                             count(m.read) filter ( where m.read = false and m.sender_type = 'SUPPORT') as unread
                      from message m
                      group by m.chat_id) msg
                         left join chat ch
                                   on ch.id = msg.chat_id
                         left join message m2 on msg.id = m2.id
                where ch.client_id = :userId
                order by m2.created_date desc
                limit :pageSize offset :offset
    """

        val paramMap = mapOf(
            "userId" to userId,
            "pageSize" to pageable.pageSize,
            "offset" to pageable.offset
        )

        return namedParameterJdbcTemplate.query(sql, paramMap) { rs, _ -> mapToSupportResultQueryDto(rs) }
    }

    fun getAdminChatList(pageable: Pageable): List<SupportResultQueryDto> {
        val sql = """SELECT ch.appeal_section as appealSection, 
                         ch.open as open, 
                         ch.id as id, 
                         ch.client_username as clientUsername, 
                         ch.chatuid as chatUid,
                         ch.client_id as clientId, 
                         m2.message_type as type, 
                         m2.file_id as fileId, 
                         m2.text as text, 
                         m2.created_date as createdDate, 
                         m2.file_name as fileName, 
                         m2.file_size as fileSize, 
                         msg.unread as unread 
                 FROM (
                     SELECT m.chat_id, 
                            MAX(m.id) as id, 
                            MAX(m.created_date),
                            COUNT(m.read) FILTER (WHERE m.read = false AND m.sender_type='CLIENT') as unread
                     FROM message m 
                     GROUP BY m.chat_id
                 ) msg
                 LEFT JOIN chat ch ON ch.id = msg.chat_id
                 LEFT JOIN message m2 ON msg.id = m2.id 
                 ORDER BY m2.created_date DESC
                 LIMIT :pageSize OFFSET :offset"""

        val paramMap = mapOf(
            "pageSize" to pageable.pageSize,
            "offset" to pageable.offset
        )

        return namedParameterJdbcTemplate.query(sql, paramMap) { rs, _ -> mapToSupportResultQueryDto(rs) }
    }

    fun getOpenChatListForSupporter(moderatorId: Long): List<SupportResultQueryDto> {
        val sql = """SELECT ch.appeal_section as appealSection, 
                         ch.open as open, 
                         ch.id as id, 
                         ch.client_username as clientUsername, 
                         ch.chatuid as chatUid,
                         ch.client_id as clientId, 
                         m2.message_type as type, 
                         m2.file_id as fileId, 
                         m2.text as text, 
                         m2.created_date as createdDate, 
                         m2.file_name as fileName, 
                         m2.file_size as fileSize, 
                         msg.unread as unread 
                 FROM (SELECT m.chat_id, 
                              max(m.id) as id, 
                              max(m.created_date), 
                              count(m.read) filter (where m.read = false and m.sender_type = 'CLIENT') as unread
                       FROM message m 
                       GROUP BY m.chat_id) msg
                 LEFT JOIN chat ch ON ch.id = msg.chat_id
                 LEFT JOIN message m2 ON msg.id = m2.id 
                 WHERE ch.moderator_id = :moderatorId AND ch.open = true 
                 ORDER BY m2.created_date DESC"""

        val paramMap = mapOf("moderatorId" to moderatorId)
        return namedParameterJdbcTemplate.query(sql, paramMap) { rs, _ -> mapToSupportResultQueryDto(rs) }
    }

    private fun mapToSupportResultQueryDto(rs: ResultSet): SupportResultQueryDto {
        var dateString = rs.getString("createdDate")

        if (!dateString.contains(".")) {
            dateString = dateString.plus(".000")
        }
        val dateFormat = SimpleDateFormat(FORMAT_DATE)
        val createdDate: Date = dateFormat.parse(dateString)
        return SupportResultQueryDto(
            appealSection = AppealSection.valueOf(rs.getString("appealSection")!!),
            open = rs.getBoolean("open"),
            chatUid = rs.getString("chatUid"),
            clientId = rs.getString("clientId"),
            type = MessageType.valueOf(rs.getString("type")!!),
            fileId = rs.getString("fileId"),
            text = rs.getString("text"),
            createdDate = createdDate.time,
            unread = rs.getInt("unread"),
            clientUsername = rs.getString("clientUsername"),
            fileName = rs.getString("fileName"),
            fileSize = rs.getString("fileSize")
        )
    }

    companion object {
        private const val FORMAT_DATE = "yyyy-MM-dd HH:mm:ss.SSS"
    }
}
