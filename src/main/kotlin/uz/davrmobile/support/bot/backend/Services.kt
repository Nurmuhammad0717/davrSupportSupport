package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*
import javax.transaction.Transactional
import kotlin.math.round

interface UserService {
    fun getAllUsers(): List<UserResponse>
    fun deleteUser(userId: Long)
    fun getUserById(id: Long): UserResponse
}

interface SessionService {
    fun getAllSession(pageable: Pageable): Page<SessionInfo>
    fun getOne(id: Long): SessionInfo
    fun getAllSessionUser(userId: Long, pageable: Pageable): Page<SessionInfo>
    fun getAllSessionOperator(operatorId: Long, pageable: Pageable): Page<SessionInfo>
    fun getAllSessionUserDateRange(userId: Long, dto: DateRangeRequest, pageable: Pageable): Page<SessionInfo>
    fun getAllSessionOperatorDateRange(operatorId: Long, dto: DateRangeRequest, pageable: Pageable): Page<SessionInfo>
    fun getSessionsByStatus(status: SessionStatusEnum, pageable: Pageable): Page<SessionInfo>
    fun getHighRateOperator(pageable: Pageable): Page<RateInfo>
    fun getLowRateOperator(pageable: Pageable): Page<RateInfo>
    fun getHighRateOperatorDateRange(dto: DateRangeRequest, pageable: Pageable): Page<RateInfo>
    fun getLowRateOperatorDateRange(dto: DateRangeRequest, pageable: Pageable): Page<RateInfo>
    fun getOperatorRate(operatorId: Long, pageable: Pageable): Page<RateInfo>
}

interface FileInfoService {
    fun upload(multipartFile: MultipartFile)
}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun getAllUsers(): List<UserResponse> {
        return userRepository.findAllByDeletedFalse().map {
            UserResponse.toResponse(it)
        }
    }

    override fun deleteUser(userId: Long) {
        val optional = userRepository.findById(userId)
        if (!optional.isPresent) throw UserNotFoundException()
        val user = optional.get()
        user.deleted = true
        userRepository.save(user)
    }

    override fun getUserById(id: Long): UserResponse {
        userRepository.findByIdAndDeletedFalse(id)?.let {
            return UserResponse.toResponse(it)
        } ?: throw UserNotFoundException()
    }
}

@Service
class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
) : SessionService {

    override fun getAllSession(pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.findAll(pageable))
    }

    override fun getOne(id: Long): SessionInfo {
        val session = sessionRepository.findById(id)
            .orElseThrow { SessionNotFoundExistException() }
        return toSessionInfo(session)
    }

    override fun getAllSessionUser(userId: Long, pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.getSessionByUserId(userId, pageable))
    }


    override fun getAllSessionOperator(operatorId: Long, pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.getSessionByOperatorId(operatorId, pageable))
    }

    override fun getAllSessionUserDateRange(
        userId: Long,
        dto: DateRangeRequest,
        pageable: Pageable
    ): Page<SessionInfo> {
        return toSessionInfo(
            sessionRepository.findAllSessionsByUserAndDateRange(
                userId,
                dto.fromDate,
                dto.toDate,
                pageable
            )
        )
    }

    override fun getAllSessionOperatorDateRange(
        operatorId: Long,
        dto: DateRangeRequest,
        pageable: Pageable
    ): Page<SessionInfo> {
        return toSessionInfo(
            sessionRepository.findAllSessionsByOperatorAndDateRange(
                operatorId,
                dto.fromDate,
                dto.toDate,
                pageable
            )
        )
    }

    override fun getSessionsByStatus(status: SessionStatusEnum, pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.getSessionByStatus(status, pageable))
    }

    override fun getHighRateOperator(pageable: Pageable): Page<RateInfo> {
        return toRateInfo(sessionRepository.findHighestRatedOperators(pageable))
    }

    override fun getLowRateOperator(pageable: Pageable): Page<RateInfo> {
        return toRateInfo(sessionRepository.findLowestRatedOperators(pageable))
    }

    override fun getHighRateOperatorDateRange(dto: DateRangeRequest, pageable: Pageable): Page<RateInfo> {
        return toRateInfo(sessionRepository.findHighestRatedOperatorsByDateRange(dto.fromDate, dto.toDate, pageable))
    }

    override fun getLowRateOperatorDateRange(dto: DateRangeRequest, pageable: Pageable): Page<RateInfo> {
        return toRateInfo(sessionRepository.findLowestRatedOperatorsByDateRange(dto.fromDate, dto.toDate, pageable))

    }

    override fun getOperatorRate(operatorId: Long, pageable: Pageable): Page<RateInfo> {
        return toRateInfo(sessionRepository.findOperatorRates(operatorId, pageable))
    }

    private fun toSessionInfo(sessions: Page<Session>): Page<SessionInfo> {
        return sessions.map { session ->
            SessionInfo(
                user = UserResponse.toResponse(session.user),
                status = session.status!!,
                operatorId = session.operatorId,
                rate = session.rate
            )
        }
    }

    private fun toSessionInfo(session: Session): SessionInfo {
        return SessionInfo(
            user = UserResponse.toResponse(session.user),
            status = session.status!!,
            operatorId = session.operatorId,
            rate = session.rate
        )
    }

    private fun toRateInfo(results: Page<Array<Any>>): Page<RateInfo> {
        return results.map { result ->
            val operator = result[0] as BotUser
            val totalRate = result[1] as Number
            val roundedRate = round(totalRate.toDouble() * 100) / 100
            RateInfo(rate = roundedRate, operator = UserResponse.toResponse(operator))
        }
    }
}

interface MessageToOperatorService {

    fun getSessions(): List<SessionResponse>
    fun getSessionMessages(sessionId: Long): SessionMessagesResponse
    fun getUnreadMessages(sessionId: Long): SessionMessagesResponse
    fun sendMessage()

}

@Service
class MessageToOperatorServiceImpl(

    private val userService: UserService,
    private val sessionRepository: SessionRepository,
    private val botMessageRepository: BotMessageRepository

) : MessageToOperatorService {
    override fun getSessions(): List<SessionResponse> {
        val waitingSessions = sessionRepository.findAllByStatusAndDeletedFalse(SessionStatusEnum.WAITING)
        return waitingSessions.map {
            val count = botMessageRepository.findAllBySessionIdAndHasReadFalseAndDeletedFalse(it.id!!).count()
            SessionResponse.toResponse(it, count)
        }
    }

    override fun getSessionMessages(sessionId: Long): SessionMessagesResponse {
        val sessionOpt = sessionRepository.findById(sessionId)
        if (sessionOpt.isPresent) {
            val messages = botMessageRepository.findAllBySessionIdAndDeletedFalse(sessionId)
            return SessionMessagesResponse(
                sessionId,
                UserResponse.toResponse(sessionOpt.get().user),
                messages.map { BotMessageResponse.toResponse(it) })
        }
        throw SessionNotFoundExistException()
    }

    @Transactional
    override fun getUnreadMessages(sessionId: Long): SessionMessagesResponse {
        val sessionOptional = sessionRepository.findById(sessionId)
        if (sessionOptional.isPresent) {
            val unreadMessages =
                botMessageRepository.findAllBySessionIdAndHasReadFalseAndDeletedFalse(sessionId)
            for (unreadMessage in unreadMessages) {
                unreadMessage.hasRead = true
            }
            botMessageRepository.saveAll(unreadMessages)
            return SessionMessagesResponse(
                sessionId,
                UserResponse.toResponse(sessionOptional.get().user),
                unreadMessages.map { BotMessageResponse.toResponse(it) })
        }
        throw SessionNotFoundExistException()
    }

    override fun sendMessage() {
        TODO("Not yet implemented")
    }
}

