package uz.davrmobile.support.usecase

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.ResultUnboundMessagesDto
import uz.davrmobile.support.enm.AppealSection

@Component
class GetUnboundChatsUseCase(
    private val jdbcTemplate: JdbcTemplate
) {

    fun execute(sections: List<AppealSection>?, pageable: Pageable): Page<ResultUnboundMessagesDto> {
        val filter = StringBuilder("WHERE ch.moderator_id IS NULL ")
        if (!sections.isNullOrEmpty()) {
            val sectionFilter = sections.joinToString(", ") { "'${it.name}'" }
            filter.append("AND ch.appeal_section IN ($sectionFilter) ")
        }

        val offset = pageable.offset
        val pageSize = pageable.pageSize
        val sql = """
            SELECT ch.appeal_section  AS appealSection,
                   ch.open            AS open,
                   ch.client_username AS username,
                   ch.chatuid         AS chatUid,
                   ch.client_id       AS clientId,
                   m2.message_type    AS type,
                   m2.file_id         AS fileId,
                   m2.text            AS text,
                   m2.created_date    AS createdDate,
                   msg.unread         AS unread,
                   m2.file_name       AS fileName,
                   m2.file_size       AS fileSize
            FROM (SELECT m.chat_id,
                         max(m.id)                                                                AS id,
                         max(m.created_date),
                         count(m.read) FILTER (WHERE m.read = false AND m.sender_type = 'CLIENT') AS unread
                  FROM message m
                  GROUP BY m.chat_id) msg
                     LEFT JOIN chat ch ON ch.id = msg.chat_id
                     LEFT JOIN message m2 ON msg.id = m2.id
            $filter
            ORDER BY ch.created_date DESC
            LIMIT $pageSize OFFSET $offset
        """.trimIndent()

        val results = jdbcTemplate.query(
            sql
        ) { rs, _ ->
            ResultUnboundMessagesDto(
                appealSection = AppealSection.valueOf(rs.getString("appealSection")),
                open = rs.getBoolean("open"),
                username = rs.getString("username"),
                chatUid = rs.getString("chatUid"),
                clientId = rs.getString("clientId"),
                type = rs.getString("type"),
                fileId = rs.getString("fileId"),
                text = rs.getString("text"),
                createdDate = rs.getDate("createdDate"),
                unread = rs.getInt("unread"),
                fileName = rs.getString("fileName"),
                fileSize = rs.getString("fileSize")
            )
        }

        val countSql = """
            SELECT COUNT(*)
            FROM chat ch
            $filter
        """.trimIndent()

        val total = jdbcTemplate.queryForObject(countSql, Long::class.java) ?: 0

        return PageImpl(results, pageable, total)
    }
}

