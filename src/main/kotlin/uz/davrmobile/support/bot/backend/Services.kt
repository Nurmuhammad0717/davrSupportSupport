package uz.davrmobile.support.bot.backend

import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.*
import org.telegram.telegrambots.meta.api.methods.send.SendContact
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendLocation
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot
import uz.davrmobile.support.util.getUserId
import java.io.File
import java.io.FileInputStream
import javax.servlet.http.HttpServletResponse
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
    fun download(hashId: String, response: HttpServletResponse)
    fun find(hashId: String): FileInfoResponse
    fun findAll(pageable: Pageable): Page<FileInfoResponse>
    fun upload(multipartFileList: MutableList<MultipartFile>): List<FileInfoResponse>
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
        val session = sessionRepository.findById(id).orElseThrow { SessionNotFoundException() }
        return toSessionInfo(session)
    }

    override fun getAllSessionUser(userId: Long, pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.getSessionByUserId(userId, pageable))
    }


    override fun getAllSessionOperator(operatorId: Long, pageable: Pageable): Page<SessionInfo> {
        return toSessionInfo(sessionRepository.getSessionByOperatorId(operatorId, pageable))
    }

    override fun getAllSessionUserDateRange(
        userId: Long, dto: DateRangeRequest, pageable: Pageable
    ): Page<SessionInfo> {
        return toSessionInfo(
            sessionRepository.findAllSessionsByUserAndDateRange(
                userId, dto.fromDate, dto.toDate, pageable
            )
        )
    }

    override fun getAllSessionOperatorDateRange(
        operatorId: Long, dto: DateRangeRequest, pageable: Pageable
    ): Page<SessionInfo> {
        return toSessionInfo(
            sessionRepository.findAllSessionsByOperatorAndDateRange(
                operatorId, dto.fromDate, dto.toDate, pageable
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
    fun getSessionMessages(id: String): SessionMessagesResponse
    fun getUnreadMessages(id: String): SessionMessagesResponse
    fun sendMessage(message: OperatorSentMsgRequest)
    fun closeSession(sessionHash: String)
}

@Service
class MessageToOperatorServiceImpl(
    private val sessionRepository: SessionRepository,
    private val botMessageRepository: BotMessageRepository,
    private val botRepository: BotRepository,
    private val fileInfoRepository: FileInfoRepository

) : MessageToOperatorService {
    override fun getSessions(): List<SessionResponse> {
        val waitingSessions = sessionRepository.findAllByStatusAndDeletedFalse(SessionStatusEnum.WAITING)
        return waitingSessions.map {
            val count = botMessageRepository.countAllBySessionIdAndHasReadFalseAndDeletedFalse(it.id!!)
            val bot = botRepository.findById(it.botId).get()
            SessionResponse.toResponse(it, count, bot)
        }
    }

    override fun getSessionMessages(id: String): SessionMessagesResponse {
        sessionRepository.findByHashId(id)?.let { session ->
            val messages = botMessageRepository.findAllBySessionIdAndDeletedFalse(session.id!!)
            return SessionMessagesResponse(
                session.hashId,
                UserResponse.toResponse(session.user),
                messages.map { BotMessageResponse.toResponse(it) })
        }
        throw SessionNotFoundException()
    }

    @Transactional
    override fun getUnreadMessages(id: String): SessionMessagesResponse {
        sessionRepository.findByHashId(id)?.let { session ->
            val unreadMessages = botMessageRepository.findAllBySessionIdAndHasReadFalseAndDeletedFalse(session.id!!)
            for (unreadMessage in unreadMessages) {
                unreadMessage.hasRead = true
            }
            botMessageRepository.saveAll(unreadMessages)
            return SessionMessagesResponse(
                session.hashId,
                UserResponse.toResponse(session.user),
                unreadMessages.map { BotMessageResponse.toResponse(it) })
        }
        throw SessionNotFoundException()
    }

    @Transactional
    override fun sendMessage(message: OperatorSentMsgRequest) {
        val sessionHashId = message.sessionId!!
        sessionRepository.findByHashId(sessionHashId)?.let { session ->
            if(session.operatorId==null){
                session.operatorId = getUserId()
                session.status = SessionStatusEnum.BUSY
                sessionRepository.save(session)
            } else if(session.operatorId!= getUserId()) throw BusySessionException()
            val user = session.user
            val userId = user.id.toString()
            botRepository.findByIdAndDeletedFalse(session.botId)?.let { bot ->
                SupportTelegramBot.findBotById(bot.id!!)?.let { absSender ->
                    when (message.type!!) {
                        BotMessageType.TEXT -> {
                            message.text?.let {
                                absSender.execute(SendMessage(userId, it))
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.VIDEO, BotMessageType.PHOTO, BotMessageType.VOICE, BotMessageType.AUDIO, BotMessageType.DOCUMENT, BotMessageType.ANIMATION -> {
                            message.fileId?.let { fileHashIds ->
                                val inputMediaList: MutableList<InputMedia> = mutableListOf()
                                for (fileHashId in fileHashIds) {
                                    val fileInfo = fileInfoRepository.findByHashId(fileHashId)!!
                                    inputMediaList.add(getInputMediaByFileInfo(fileInfo, message.caption))
                                }
                                sendMediaGroup(userId, inputMediaList, absSender, message.caption)
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.LOCATION -> {
                            message.location?.let {
                                absSender.execute(SendLocation(userId, it.latitude, it.longitude))
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.CONTACT -> {
                            message.contact?.let {
                                absSender.execute(SendContact(userId, it.phoneNumber, it.name))
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.POLL -> {}
                        BotMessageType.DICE -> {}
                        BotMessageType.STICKER -> {}
                        BotMessageType.VIDEO_NOTE -> {}
                    }
                } ?: throw BotNotFoundException()
            } ?: throw BotNotFoundException()
        } ?: throw SessionNotFoundException()
    }

    private fun sendMediaGroup(
        userId: String, inputMediaList: MutableList<InputMedia>, absSender: SupportTelegramBot, caption: String?
    ) {
        if (inputMediaList.size == 1) {
            val media = inputMediaList[0]
            val inputFile = InputFile().setMedia(File(media.media))

            when (media) {
                is InputMediaAnimation -> {
                    val send = SendAnimation()
                    send.chatId = userId
                    send.animation = inputFile
                    send.caption = caption
                    absSender.execute(send)
                }

                is InputMediaVideo -> {
                    val send = SendVideo()
                    send.chatId = userId
                    send.video = inputFile
                    send.caption = caption
                    absSender.execute(send)
                }

                is InputMediaPhoto -> {
                    val send = SendPhoto(userId, inputFile)
                    send.chatId = userId
                    send.photo = inputFile
                    send.caption = caption
                    absSender.execute(send)
                }

                is InputMediaAudio -> {
                    val send = SendAudio()
                    send.chatId = userId
                    send.audio = inputFile
                    send.caption = caption
                    absSender.execute(send)
                }

                else -> {
                    val send = SendDocument()
                    send.chatId = userId
                    send.document = inputFile
                    send.caption = caption
                    absSender.execute(send)
                }
            }
        } else if (inputMediaList.size > 10) {
            inputMediaList.chunked(10).map { inputMedia ->
                sendMediaGroup(userId, inputMedia.toMutableList(), absSender, caption)
            }
        } else {
            absSender.execute(SendMediaGroup(userId, inputMediaList))
        }
    }

    private fun getInputMediaByFileInfo(fileInfo: FileInfo, caption: String?): InputMedia {
        val filePath = File(Paths.get(fileInfo.path).toAbsolutePath().toString())
        val fileName = fileInfo.name
        val extension = fileInfo.extension.lowercase()
        val inputMedia = when (extension) {
            "gif" -> InputMediaAnimation().apply { }
            "mp4", "mov", "avi" -> InputMediaVideo()
            "jpg", "jpeg", "png", "webp" -> InputMediaPhoto()
            "mp3", "m4a", "ogg", "flac", "wav" -> InputMediaAudio()
            else -> InputMediaDocument()
        }
        inputMedia.setMedia(filePath.inputStream(), filePath.toString().substringBeforeLast("/"))
        inputMedia.caption = caption
        return inputMedia
    }

    override fun closeSession(sessionHash: String) {
        sessionRepository.findByHashId(sessionHash)?.let {
            if(it.isClosed()) return
            it.status = SessionStatusEnum.CLOSED
            sessionRepository.save(it)
        }
    }
}

@Service
class FileInfoServiceImpl(private val fileInfoRepository: FileInfoRepository) : FileInfoService {

    private val path: String = "files/${LocalDate.now()}"

    override fun upload(multipartFileList: MutableList<MultipartFile>): List<FileInfoResponse> {
        val responseFiles: MutableList<FileInfoResponse> = mutableListOf()
        multipartFileList.forEach { multipartFile ->
            val name = takeFileName(multipartFile)
            val fileInfo = FileInfo(
                name = name,
                uploadName = multipartFile.originalFilename!!,
                extension = FilenameUtils.getExtension(multipartFile.originalFilename),
                path = getFilePath(name).toString(),
                size = multipartFile.size
            )
            val savedFileInfo = fileInfoRepository.save(fileInfo)

            val filePath = Paths.get(fileInfo.path)
            filePath.parent?.let { directoryPath ->
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath)
                }
            }
            multipartFile.inputStream.use { inputStream ->
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
            }
            responseFiles.add(FileInfoResponse.toResponse(savedFileInfo))
        }
        return responseFiles
    }

    override fun download(hashId: String, response: HttpServletResponse) {
        val fileDB = fileInfoRepository.findByHashId(hashId)?: throw FileNotFoundException()
        val path: Path = Paths.get(fileDB.path).normalize()
        val file = path.toFile()
        response.contentType = Files.probeContentType(path) ?: "application/octet-stream"
        response.setHeader("Content-Disposition", "attachment; filename=${file.name}")
        response.setContentLengthLong(file.length())

        FileInputStream(file).use { inputStream ->
            response.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    override fun find(hashId: String): FileInfoResponse {
        val file = fileInfoRepository.findByHashId(hashId) ?: throw FileNotFoundException()
        return FileInfoResponse.toResponse(file)
    }

    override fun findAll(pageable: Pageable): Page<FileInfoResponse> {
        val result = fileInfoRepository.findAll(pageable)
        return result.map { FileInfoResponse.toResponse(it) }
    }

    private fun getFilePath(name: String): Path {
        return Paths.get(path, name)
    }

    private fun takeFileName(multipartFile: MultipartFile): String {
        return "${FilenameUtils.removeExtension(multipartFile.originalFilename)}:${Date().time}.${FilenameUtils.getExtension(multipartFile.originalFilename)}"
    }
}
