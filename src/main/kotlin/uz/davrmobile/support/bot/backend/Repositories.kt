package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uz.davrmobile.support.repository.BaseRepository
import java.util.*


interface DiceRepository : BaseRepository<Dice>

interface UserRepository : JpaRepository<BotUser, Long> {
    fun findAllByDeletedFalseOrderByIdDesc(pageable: Pageable): Page<BotUser>
    fun findAllByDeletedFalseAndFullNameContainsIgnoreCaseOrderByIdDesc(
        pageable: Pageable,
        fullName: String
    ): Page<BotUser>

    fun findByIdAndDeletedFalse(id: Long): BotUser?
}

interface BotMessageRepository : BaseRepository<BotMessage> {
    fun findAllBySessionIdAndDeletedFalse(sessionId: Long, pageable: Pageable): Page<BotMessage>
    fun findAllBySessionIdAndHasReadFalseAndDeletedFalse(sessionId: Long, pageable: Pageable): Page<BotMessage>
    fun findByUserIdAndMessageId(userId: Long, messageId: Int): BotMessage?
    fun findFirstBySessionOrderByCreatedDateDesc(session: Session) : BotMessage?
    fun findByMessageIdAndSessionAndDeletedFalse(messageId: Int, session: Session): BotMessage?
    @Query(
        """
            SELECT NEW map(m.session as session, m as message)
        FROM bot_message m
        WHERE m.deleted = false
        ORDER BY m.session.id ASC, m.id ASC
    """
    )
    fun findByMessageIdAndDeletedFalse(messageId: Int): BotMessage?
}

@Repository
interface SessionRepository : BaseRepository<Session> {

    @Query(
        """
    SELECT 
        COUNT(CASE WHEN m.hasRead = false THEN 1 END) AS unreadMessageCount, 
        b AS bot
    FROM Session s
    LEFT JOIN bot_message m ON m.session.id = s.id
    LEFT JOIN Bot b ON b.chatId = s.botId
    WHERE s.id = :sessionId
    GROUP BY s.id,b.id
    """
    )
    fun getSessionLastMessageWithUnreadCount(
        @Param("sessionId") sessionId: Long
    ): List<LastMessageWithCount>


    @Query(
        value = """
    SELECT 
        s.id AS id,
        s.rate AS rate,
        CASE
            WHEN s.language = 0 THEN 'UZ'
            WHEN s.language = 1 THEN 'RU'
            WHEN s.language = 2 THEN 'EN'
        END AS language,
        s.created_date AS date,
        s.status AS status,
        b.hash_id AS botId,
        b.username AS botUsername,
        b.name AS botName,
        u.id AS userId,
        u.full_name AS userFullName
    FROM session s
    LEFT JOIN bot b ON b.chat_id = s.bot_id
    LEFT JOIN bot_user u ON u.id = s.user_id
    WHERE s.operator_id = :operatorId
      AND s.status = 'CLOSED'
    """,
        nativeQuery = true
    )
    fun findClosedSessions(pageable: Pageable, @Param("operatorId") operatorId: Long): Page<ClosedSessionProjection>


    @Query(
        value = """
            SELECT *
            FROM session s
            WHERE s.bot_id IN (SELECT b.chat_id
                               FROM bot b
                                        JOIN bot_operator_ids boi ON b.id = boi.bot_id
                               WHERE b.status = ?1
                                 AND b.deleted = FALSE
                                 AND boi.operator_ids = ?2)
              AND s.deleted = FALSE
              AND s.status = ?3
              AND s.language IN (?4)
                """,
        countQuery = """
            SELECT count(*)
            FROM session s
            WHERE s.bot_id IN (SELECT b.chat_id
                               FROM bot b
                                        JOIN bot_operator_ids boi ON b.id = boi.bot_id
                               WHERE b.status = ?1
                                 AND b.deleted = FALSE
                                 AND boi.operator_ids = ?2)
              AND s.deleted = FALSE
              AND s.status = ?3
              AND s.language IN (?4)
    """,
        nativeQuery = true
    )
    fun getWaitingSessions(
        botStatus: String,   // ?1
        operatorId: Long,           // ?2
        status: String,  // ?3
        languages: List<Int>, // ?4
        pageable: Pageable
    ): Page<Session>

    fun findAllByOperatorIdAndStatus(operatorId: Long, status: SessionStatusEnum, pageable: Pageable): Page<Session>
    override fun findByIdAndDeletedFalse(id: Long): Session?

    @Query(
        value = "SELECT * FROM session s " +
                "WHERE s.user_id = :userId " +
                "ORDER BY s.created_date DESC LIMIT 1",
        nativeQuery = true
    )
    fun findLastSessionByUserId(@Param("userId") userId: Long): Session?
    fun findByHashId(hashId: String): Session?

    @Query(
        """
    SELECT 
        COALESCE(s.operator_id,0) AS operatorId,
        COALESCE(COUNT(DISTINCT s.id),0) AS sessionCount,
        COALESCE(COUNT(m.id),0) AS messageCount,
        COALESCE(AVG(s.rate), 0) AS avgRate
    FROM session s
    LEFT JOIN bot_message m ON s.id = m.session_id
    WHERE s.operator_id = :operatorId 
      AND DATE(s.created_date) BETWEEN :startDate and :endDate
    GROUP BY s.operator_id
    """,
        nativeQuery = true
    )
    fun findSessionInfoByOperatorIdDateRange(
        startDate: Date,
        endDate: Date,
        operatorId: Long
    ): SessionInfoByOperatorResponse?

    @Query(
        """
    SELECT 
        COALESCE(s.operator_id,0) AS operatorId,
        COALESCE(COUNT(DISTINCT s.id),0) AS sessionCount,
        COALESCE(COUNT(m.id),0) AS messageCount,
        COALESCE(AVG(s.rate), 0) AS avgRate
    FROM session s
    LEFT JOIN bot_message m ON s.id = m.session_id
    WHERE s.operator_id = :operatorId 
      AND DATE(s.created_date) = :thisDate
    GROUP BY s.operator_id
    """,
        nativeQuery = true
    )
    fun findSessionInfoByOperatorIdAndDate(
        @Param("thisDate") thisDate: Date,
        @Param("operatorId") operatorId: Long
    ): SessionInfoByOperatorResponse?

    @Query(
        """
    SELECT 
        COALESCE(s.operator_id,0) AS operatorId,
        COALESCE(COUNT(DISTINCT s.id),0) AS sessionCount,
        COALESCE(COUNT(m.id),0) AS messageCount,
        COALESCE(AVG(s.rate), 0) AS avgRate
    FROM session s
    LEFT JOIN bot_message m ON s.id = m.session_id
    WHERE s.operator_id = :operatorId
    GROUP BY s.operator_id
    """,
        nativeQuery = true
    )
    fun findSessionInfoByOperatorId(operatorId: Long): SessionInfoByOperatorResponse?


}

interface LocationRepository : BaseRepository<Location>

interface ContactRepository : BaseRepository<Contact>

interface BotRepository : BaseRepository<Bot> {
    fun findAllByStatus(status: BotStatusEnum): List<Bot>
    fun findAllBotsByStatusAndDeletedFalse(status: BotStatusEnum, pageable: Pageable): Page<Bot>
    fun findByHashId(hashId: String): Bot?
    fun findAllByDeletedFalse(pageable: Pageable): Page<Bot>
    fun findAllBotsByStatusAndDeletedFalse(pageable: Pageable, status: BotStatusEnum): Page<Bot>
    fun existsByToken(token: String): Boolean
    fun findAllBotsByOperatorIdsContains(operatorIds: Long): List<Bot>
    fun findByChatIdAndDeletedFalse(chatId: Long): Bot?
    fun findByHashIdAndDeletedFalse(id: String): Bot?
    fun deleteByHashId(hashId: String)
}

interface FileInfoRepository : BaseRepository<FileInfo> {
    fun findByHashId(hashId: String): FileInfo?
    fun findAllByHashIdIn(hashIds: List<String>): MutableList<FileInfo>
}

interface OperatorLanguageRepository : BaseRepository<OperatorLanguage>

interface StandardAnswerRepository : BaseRepository<StandardAnswer> {
    fun existsByText(text: String): Boolean
    fun existsByTextAndIdNot(text: String, id: Long): Boolean
}

