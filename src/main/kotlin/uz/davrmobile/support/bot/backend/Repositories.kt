package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uz.davrmobile.support.repository.BaseRepository
import java.util.*


interface DiceRepository : BaseRepository<Dice>

interface UserRepository : JpaRepository<BotUser, Long> {
    fun findAllByDeletedFalse(): List<BotUser>
    fun findByIdAndDeletedFalse(id: Long): BotUser?
}

interface BotMessageRepository : BaseRepository<BotMessage> {
    fun findAllBySessionIdAndDeletedFalse(sessionId: Long): List<BotMessage>
    fun findAllBySessionIdAndHasReadFalseAndDeletedFalse(sessionId: Long): List<BotMessage>

    fun countAllBySessionIdAndHasReadFalseAndDeletedFalse(sessionId: Long): Int

    fun findByUserIdAndMessageId(userId: Long, messageId: Int): BotMessage?

    @Query(
        """
            SELECT NEW map(m.session as session, m as message)
        FROM bot_message m
        WHERE m.deleted = false
        ORDER BY m.session.id ASC, m.id ASC
    """
    )
    fun findMessagesGroupedBySessionId(): List<Map<Any, Any>>
    abstract fun findAllByHashIdAndHasReadFalseAndDeletedFalse(hashId: String): List<BotMessage>
}

interface SessionRepository : BaseRepository<Session> {
    fun findAllByBotIdInAndDeletedFalseAndStatusAndLanguageIn(
        botIds: List<Long>,
        status: SessionStatusEnum,
        languages: List<LanguageEnum>,
        pageable: Pageable
    ): Page<Session>

    fun findAllByOperatorIdAndStatus(operatorId: Long, status: SessionStatusEnum): List<Session>

    fun findAllByStatusAndDeletedFalse(status: SessionStatusEnum): List<Session>

    fun findAllByOperatorId(operatorId: Long, pageable: Pageable): Page<Session>

    override fun findByIdAndDeletedFalse(id: Long): Session?

    @Query(
        """
    SELECT s.operatorId, SUM(s.rate)
    FROM Session s 
    WHERE s.rate IS NOT NULL 
      AND s.createdDate BETWEEN :fromDate AND :toDate
    GROUP BY s.operatorId 
    ORDER BY SUM(s.rate) DESC
    """
    )
    fun findHighestRatedOperatorsByDateRange(
        @Param("fromDate") fromDate: Date,
        @Param("toDate") toDate: Date,
        pageable: Pageable
    ): Page<Array<Any>>

    @Query(
        """
    SELECT s.operatorId, SUM(s.rate)
    FROM Session s 
    WHERE s.rate IS NOT NULL 
      AND s.createdDate BETWEEN :fromDate AND :toDate
    GROUP BY s.operatorId 
    ORDER BY SUM(s.rate) ASC
    """
    )
    fun findLowestRatedOperatorsByDateRange(
        @Param("fromDate") fromDate: Date,
        @Param("toDate") toDate: Date,
        pageable: Pageable
    ): Page<Array<Any>>

    @Query(
        """
    SELECT s.operatorId, s.rate
    FROM Session s
    WHERE s.operatorId = :operatorId
      AND s.rate IS NOT NULL
    """
    )
    fun findOperatorRates(
        @Param("operatorId") operatorId: Long,
        pageable: Pageable
    ): Page<Array<Any>>

    @Query(
        """
    SELECT s
    FROM Session s
    WHERE s.operatorId = :operatorId
      AND s.createdDate BETWEEN :fromDate AND :toDate
    """
    )
    fun findAllSessionsByOperatorAndDateRange(
        @Param("operatorId") operatorId: Long,
        @Param("fromDate") fromDate: Date,
        @Param("toDate") toDate: Date,
        pageable: Pageable
    ): Page<Session>

    @Query(
        """
    SELECT s
    FROM Session s
    WHERE s.user.id = :userId
      AND s.createdDate BETWEEN :fromDate AND :toDate
    """
    )
    fun findAllSessionsByUserAndDateRange(
        @Param("userId") userId: Long,
        @Param("fromDate") fromDate: Date,
        @Param("toDate") toDate: Date,
        pageable: Pageable
    ): Page<Session>


    @Query(
        """
    SELECT s.operatorId,sum(s.rate)
    FROM Session s 
    WHERE s.rate IS NOT NULL 
    GROUP BY s.operatorId 
    ORDER BY sum(s.rate) DESC
    """
    )
    fun findHighestRatedOperators(pageable: Pageable): Page<Array<Any>>

    @Query(
        """
    SELECT s.operatorId,sum(s.rate)
    FROM Session s 
    WHERE s.rate IS NOT NULL 
    GROUP BY s.operatorId 
    ORDER BY sum(s.rate) ASC
    """
    )
    fun findLowestRatedOperators(pageable: Pageable): Page<Array<Any>>

    @Query(
        value = "SELECT * FROM session s " +
                "WHERE s.user_id = :userId " +
                "ORDER BY s.created_date DESC LIMIT 1",
        nativeQuery = true
    )
    fun findLastSessionByUserId(@Param("userId") userId: Long): Session?


    @Query(
        name = "SELECT s FROM session s " +
                "WHERE s.operatorId = :operatorId " +
                "ORDER BY s.createdDate DESC LIMIT 1", nativeQuery = true
    )
    fun findByOperatorIdAndStatus(operatorId: Long, status: SessionStatusEnum): Session?
    fun getSessionByUserId(userId: Long, pageable: Pageable): Page<Session>
    fun getSessionByOperatorId(operatorId: Long, pageable: Pageable): Page<Session>
    fun getSessionByStatus(status: SessionStatusEnum, pageable: Pageable): Page<Session>
    fun findByHashId(hashId: String): Session?

    @Query(
        """
    SELECT 
        s.operator_id AS operatorId,
        COUNT(DISTINCT s.id) AS sessionCount,
        COUNT(m.id) AS messageCount,
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
    ): SessionInfoByOperatorResponse

    @Query(
        """
    SELECT 
        s.operator_id AS operatorId,
        COUNT(DISTINCT s.id) AS sessionCount,
        COUNT(m.id) AS messageCount,
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
    ): SessionInfoByOperatorResponse

    @Query(
        """
    SELECT 
        s.operator_id AS operatorId,
        COUNT(DISTINCT s.id) AS sessionCount,
        COUNT(m.id) AS messageCount,
        COALESCE(AVG(s.rate), 0) AS avgRate
    FROM session s
    LEFT JOIN bot_message m ON s.id = m.session_id
    WHERE s.operator_id = :operatorId
    GROUP BY s.operator_id
    """,
        nativeQuery = true
    )
    fun findSessionInfoByOperatorId(operatorId: Long): SessionInfoByOperatorResponse


}

interface LocationRepository : BaseRepository<Location>

interface ContactRepository : BaseRepository<Contact>

interface BotRepository : BaseRepository<Bot> {
    fun findAllByStatus(status: BotStatusEnum): List<Bot>
    fun findAllBotsByStatusAndDeletedFalse(status: BotStatusEnum): List<Bot>
    fun findByHashId(hashId: String): Bot?
    fun deleteByHashId(id: String)
    fun findByHashIdAndDeletedFalse(id: String): Bot?
    fun findByIdAndStatusAndDeletedFalse(id: Long, status: BotStatusEnum): Bot?
    fun findAllByDeletedFalse(): List<Bot>
}

interface FileInfoRepository : BaseRepository<FileInfo> {
    fun findByHashId(hashId: String): FileInfo?
}

interface OperatorLanguageRepository : BaseRepository<OperatorLanguage>

interface StandardAnswerRepository : BaseRepository<StandardAnswer> {
    fun existsByText(text: String): Boolean
    fun existsByTextAndIdNot(text: String, id: Long): Boolean
}

