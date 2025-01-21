package uz.davrmobile.support.bot.backend

import javax.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
//    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

//    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): List<T> = findAll(isNotDeletedSpecification, pageable).content
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

interface DiceRepository : BaseRepository<Dice> {}

interface UserRepository : JpaRepository<BotUser, Long> {
    fun findAllByDeletedFalse(): List<BotUser>
    fun findByIdAndDeletedFalse(id: Long): BotUser?
}

interface BotMessageRepository : BaseRepository<BotMessage> {
    fun findByUserIdAndMessageId(userId: Long, messageId: Int): BotMessage?
    @Query("""
        SELECT NEW map(m.session as session, m as message)
        FROM bot_message m
        WHERE m.deleted = false
        ORDER BY m.session.id ASC, m.id ASC
    """)
    fun findMessagesGroupedBySessionId(): List<Map<Any, Any>>

}

interface SessionRepository : BaseRepository<Session> {

    fun findByIdAndDeletedFalse(sessionId: Long): Session?

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
    WHERE s.botUser.id = :userId
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
        "SELECT s FROM Session s " +
                "WHERE s.botUser.id = :userId " +
                "ORDER BY s.createdDate DESC limit 1"
    )
    fun findLastSessionByUserId(@Param("userId") userId: Long): Session?

    @Query(
        "SELECT s FROM Session s " +
                "WHERE s.operatorId = :operatorId " +
                "ORDER BY s.createdDate DESC LIMIT 1"
    )
    fun findByOperatorIdAndStatus(operatorId: Long, status: SessionStatusEnum): Session?
    fun getSessionByBotUserId(userId: Long, pageable: Pageable): Page<Session>
    fun getSessionByOperatorId(operatorId: Long, pageable: Pageable): Page<Session>
    fun getSessionByStatus(status: SessionStatusEnum, pageable: Pageable): Page<Session>

}

interface LocationRepository : BaseRepository<Location>
interface ContactRepository : BaseRepository<Contact>

interface BotRepository : BaseRepository<Bot> {
    fun findAllByStatus(status: BotStatusEnum): MutableList<Bot>
    fun findAllBotsByStatusAndDeletedFalse(status: BotStatusEnum): List<Bot>
    fun findByIdAndDeletedFalse(id: Long): Bot?
}


