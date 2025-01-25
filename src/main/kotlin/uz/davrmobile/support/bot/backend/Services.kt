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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot
import uz.davrmobile.support.util.getUserId
import java.io.File
import java.io.FileInputStream
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional

interface UserService {
    fun getAllUsers(): List<UserResponse>
    fun deleteUser(userId: Long)
    fun getUserById(id: Long): UserResponse
    fun addOperatorLanguages(languages: List<LanguageEnum>)
    fun removeOperatorLanguage(id: Long)
}

interface FileInfoService {
    fun download(hashId: String, response: HttpServletResponse)
    fun show(hashId: String, response: HttpServletResponse)
    fun find(hashId: String): FileInfoResponse
    fun findAll(pageable: Pageable): Page<FileInfoResponse>
    fun upload(multipartFileList: MutableList<MultipartFile>): List<FileInfoResponse>
}

interface StandardAnswerService {
    fun create(request: StandardAnswerRequest): StandardAnswerResponse
    fun update(request: StandardAnswerUpdateRequest, id: Long): StandardAnswerResponse
    fun find(id: Long): StandardAnswerResponse
    fun findAll(pageable: Pageable): Page<StandardAnswerResponse>
    fun delete(id: Long)
}

interface StatisticService {
    fun getSessionByOperatorDateRange(operatorId: Long?, startDate: Date, endDate: Date): SessionInfoByOperatorResponse
    fun getSessionByOperatorDateRange(operatorId: Long?, date: Date): SessionInfoByOperatorResponse
    fun getSessionByOperatorDateRange(operatorId: Long?): SessionInfoByOperatorResponse
}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository, private val operatorLanguageRepository: OperatorLanguageRepository
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

    override fun addOperatorLanguages(languages: List<LanguageEnum>) {
        val userId = getUserId()
        for (it in languages) {
            operatorLanguageRepository.save(OperatorLanguage(userId, it))
        }
    }

    override fun removeOperatorLanguage(id: Long) {
        operatorLanguageRepository.trash(id) ?: throw OperatorLanguageNotFoundException()
    }
}

interface MessageToOperatorService {
    fun getSessions(request: GetSessionRequest, pageable: Pageable): GetSessionsResponse
    fun getSessionMessages(id: String): SessionMessagesResponse
    fun getUnreadMessages(id: String): SessionMessagesResponse
    fun sendMessage(message: OperatorSentMsgRequest)
    fun closeSession(sessionHash: String)
    fun editMessage(message: OperatorEditMsgRequest)
    fun editMessage(text: String?, caption: String?, msg: BotMessage)
    fun takeSession(id: String)
}

@Service
class MessageToOperatorServiceImpl(
    private val sessionRepository: SessionRepository,
    private val botMessageRepository: BotMessageRepository,
    private val botRepository: BotRepository,
    private val fileInfoRepository: FileInfoRepository,
    private val contactRepository: ContactRepository,
    private val locationRepository: LocationRepository

) : MessageToOperatorService {

    override fun getSessions(request: GetSessionRequest, pageable: Pageable): GetSessionsResponse {
        val userId = getUserId()
        val botIds: MutableList<Long> = mutableListOf()

        if (request.languages.isEmpty()) request.languages.addAll(
            mutableListOf(
                LanguageEnum.EN, LanguageEnum.RU, LanguageEnum.UZ
            )
        )

        botRepository.findAllBotsByStatusAndDeletedFalse(BotStatusEnum.ACTIVE).map {
            if (it.operatorIds.contains(userId)) botIds.add(it.id!!)
        }

        val waitingSessions = sessionRepository.findAllByBotIdInAndDeletedFalseAndStatusAndLanguageIn(
            botIds, SessionStatusEnum.WAITING, request.languages, pageable
        )
        val thisUsersBusySessions = sessionRepository.findAllByOperatorIdAndStatus(userId, SessionStatusEnum.BUSY)

        val busySessionResponse = thisUsersBusySessions.map {
            val count = botMessageRepository.countAllBySessionIdAndHasReadFalseAndDeletedFalse(it.id!!)
            val bot = botRepository.findByIdAndStatusAndDeletedFalse(it.botId, BotStatusEnum.ACTIVE)
                ?: throw BotNotFoundException()
            SessionResponse.toResponse(it, count, bot)
        }

        val waitingSessionResponse = waitingSessions.map {
            val count = botMessageRepository.countAllBySessionIdAndHasReadFalseAndDeletedFalse(it.id!!)
            val bot = botRepository.findByIdAndStatusAndDeletedFalse(it.botId, BotStatusEnum.ACTIVE)
                ?: throw BotNotFoundException()
            SessionResponse.toResponse(it, count, bot)
        }

        return GetSessionsResponse(busySessionResponse, waitingSessionResponse)
    }

    override fun getSessionMessages(id: String): SessionMessagesResponse {
        sessionRepository.findByHashId(id)?.let { session ->
            val messages = botMessageRepository.findAllBySessionIdAndDeletedFalse(session.id!!)
            return SessionMessagesResponse.toResponse(session, messages)
        }
        throw SessionNotFoundException()
    }

    @Transactional
    override fun getUnreadMessages(id: String): SessionMessagesResponse {
        sessionRepository.findByHashId(id)?.let { session ->
            val unreadMessages = botMessageRepository.findAllBySessionIdAndHasReadFalseAndDeletedFalse(session.id!!)
            for (unreadMessage in unreadMessages) unreadMessage.hasRead = true
            botMessageRepository.saveAll(unreadMessages)
            return SessionMessagesResponse.toResponse(session, unreadMessages)
        }
        throw SessionNotFoundException()
    }

    @Transactional
    override fun sendMessage(message: OperatorSentMsgRequest) {
        val operatorId = getUserId()
        val sessionHashId = message.sessionId!!
        sessionRepository.findByHashId(sessionHashId)?.let { session ->
            if (session.operatorId != getUserId()) throw BusySessionException()
            val user = session.user
            val userId = user.id.toString()
            botRepository.findByIdAndDeletedFalse(session.botId)?.let { bot ->
                SupportTelegramBot.findBotById(bot.id!!)?.let { absSender ->
                    when (message.type!!) {
                        BotMessageType.TEXT -> {
                            message.text?.let { text ->
                                val send = SendMessage(userId, text)
                                send.replyToMessageId = message.replyMessageId
                                val ex = absSender.execute(send)
                                newSessionMsg(message, session, operatorId, ex.messageId)
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.VIDEO, BotMessageType.PHOTO, BotMessageType.VOICE, BotMessageType.AUDIO, BotMessageType.DOCUMENT -> {
                            message.fileIds?.let { fileHashIds ->
                                val inputMediaList: MutableList<InputMedia> = mutableListOf()
                                for (fileHashId in fileHashIds) {
                                    val fileInfo = fileInfoRepository.findByHashId(fileHashId)!!
                                    inputMediaList.add(getInputMediaByFileInfo(fileInfo, message.caption))
                                }
                                sendMediaGroup(userId, inputMediaList, absSender, message, session, operatorId)
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.ANIMATION -> {
                            message.fileIds?.let { fileHashIds ->
                                for (fileHashId in fileHashIds) {
                                    val fileInfo = fileInfoRepository.findByHashId(fileHashId)!!
                                    val send = SendAnimation()
                                    send.chatId = userId
                                    send.animation =
                                        InputFile(File(Paths.get(fileInfo.path).toAbsolutePath().toString()))
                                    send.caption = message.caption
                                    send.replyToMessageId = message.replyMessageId
                                    val ex = absSender.execute(send)
                                    newSessionMsg(
                                        message, session, operatorId, ex.messageId, fileInfos = listOf(fileInfo)
                                    )
                                }
                            }
                        }

                        BotMessageType.LOCATION -> {
                            message.location?.let {
                                val send = SendLocation(userId, it.latitude, it.longitude)
                                send.replyToMessageId = message.replyMessageId
                                val ex = absSender.execute(send)
                                newSessionMsg(message, session, operatorId, ex.messageId, location = send)
                            } ?: throw BadCredentialsException()
                        }

                        BotMessageType.CONTACT -> {
                            message.contact?.let {
                                val send = SendContact(userId, it.phoneNumber, it.name)
                                send.replyToMessageId = message.replyMessageId
                                val ex = absSender.execute(send)
                                newSessionMsg(message, session, operatorId, ex.messageId, contact = send)
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

    private fun newSessionMsg(
        message: OperatorSentMsgRequest,
        session: Session,
        operatorId: Long,
        messageId: Int,
        fileInfos: List<FileInfo>? = null,
        location: SendLocation? = null,
        contact: SendContact? = null,
    ): BotMessage {
        return botMessageRepository.save(
            BotMessage(
                fromOperatorId = operatorId,
                session = session,
                messageId = messageId,
                replyMessageId = message.replyMessageId,
                botMessageType = message.type!!,
                text = message.text,
                caption = message.caption,
                files = fileInfos,
                location = location?.let { locationRepository.save(Location(it.latitude, it.longitude)) },
                contact = contact?.let { contactRepository.save(Contact(it.firstName, it.phoneNumber)) },
                dice = null
            )
        )
    }

    private fun sendMediaGroup(
        userId: String,
        inputMediaList: MutableList<InputMedia>,
        absSender: SupportTelegramBot,
        message: OperatorSentMsgRequest,
        session: Session,
        operatorId: Long
    ) {
        var isDocument = false
        val caption = message.caption
        val replyMessageId = message.replyMessageId
        for (inputMedia in inputMediaList) if (inputMedia is InputMediaDocument) isDocument = true
        var inputMediaListTemp: MutableList<InputMedia> = mutableListOf()
        if (isDocument) {
            for ((index, inputMedia) in inputMediaList.withIndex()) {
                val send = InputMediaDocument()
                send.setMedia(inputMedia.newMediaFile, inputMedia.mediaName)
                if (inputMediaList.size - 1 == index) send.caption = caption
                inputMediaListTemp.add(send)
            }
        } else inputMediaListTemp = inputMediaList

        if (inputMediaListTemp.size == 1) {
            val media = inputMediaListTemp[0]
            val inputFile = InputFile().apply { setMedia(media.newMediaFile, media.mediaName) }

            val ex = when (media) {
                is InputMediaAnimation -> {
                    val send = SendAnimation()
                    send.chatId = userId
                    send.animation = inputFile
                    send.caption = caption
                    send.replyToMessageId = replyMessageId
                    absSender.execute(send)
                }

                is InputMediaVideo -> {
                    val send = SendVideo()
                    send.chatId = userId
                    send.video = inputFile
                    send.caption = caption
                    send.replyToMessageId = replyMessageId
                    absSender.execute(send)
                }

                is InputMediaPhoto -> {
                    val send = SendPhoto(userId, inputFile)
                    send.chatId = userId
                    send.photo = inputFile
                    send.caption = caption
                    send.replyToMessageId = replyMessageId
                    absSender.execute(send)
                }

                is InputMediaAudio -> {
                    val send = SendAudio()
                    send.chatId = userId
                    send.audio = inputFile
                    send.caption = caption
                    send.replyToMessageId = replyMessageId
                    absSender.execute(send)
                }

                else -> {
                    val send = SendDocument().apply {
                        chatId = userId
                        document = inputFile
                        this.caption = caption
                        this.replyToMessageId = replyMessageId
                    }
                    absSender.execute(send)
                }
            }
            newSessionMsg(
                message,
                session,
                operatorId,
                ex.messageId,
                fileInfos = message.fileIds?.let { fileInfoRepository.findAllByHashIdIn(it) })
        } else if (inputMediaListTemp.size > 10) {
            inputMediaListTemp.chunked(10).map { inputMedia ->
                sendMediaGroup(userId, inputMedia.toMutableList(), absSender, message, session, operatorId)
            }
        } else {
            val send = SendMediaGroup(userId, inputMediaListTemp)
            send.replyToMessageId = replyMessageId
            val exs = absSender.execute(send)
            for (ex in exs) {
                newSessionMsg(
                    message,
                    session,
                    operatorId,
                    ex.messageId,
                    fileInfos = message.fileIds?.let { fileInfoRepository.findAllByHashIdIn(it) })
            }
        }
    }

    private fun getInputMediaByFileInfo(fileInfo: FileInfo, caption: String?): InputMedia {
        val filePath = File(Paths.get(fileInfo.path).toAbsolutePath().toString())
        val fileName = fileInfo.uploadName
        val extension = fileInfo.extension.lowercase()
        val inputMedia = when (extension) {
            "gif" -> InputMediaAnimation()
            "mp4", "mov", "avi" -> InputMediaVideo()
            "jpg", "jpeg", "png", "webp" -> InputMediaPhoto()
            "mp3", "m4a", "ogg", "flac", "wav" -> InputMediaAudio()
            else -> InputMediaDocument()
        }
        inputMedia.setMedia(filePath, fileName)
        inputMedia.caption = caption
        return inputMedia
    }

    override fun closeSession(sessionHash: String) {
        sessionRepository.findByHashId(sessionHash)?.let {
            if (it.isClosed()) return
            it.status = SessionStatusEnum.CLOSED
            sessionRepository.save(it)
            SupportTelegramBot.findBotById(it.botId)?.sendRateMsg(it.user, it)
        }
    }

    override fun editMessage(message: OperatorEditMsgRequest) {
        val msg = botMessageRepository.findByIdAndDeletedFalse(message.messageId!!) ?: throw MessageNotFoundException()
        editMessage(message.text,message.caption,msg)

    }


    override fun editMessage(text: String?, caption: String?, msg: BotMessage){
            text?.let {
                if (msg.botMessageType == BotMessageType.TEXT) {
                    if (msg.originalText == null)
                        msg.originalText = msg.text
                    msg.text = it

                    if(msg.user==null) {
                        val session = msg.session
                        val userId = session.user.id.toString()
                        SupportTelegramBot.findBotById(session.botId)?.let { bot ->
                            val send = EditMessageText()
                            send.chatId = userId
                            send.messageId = msg.messageId
                            send.text = it
                            bot.execute(send)
                        }
                    }
                }
            }

            caption?.let {
                if (msg.botMessageType in listOf(
                        BotMessageType.PHOTO, BotMessageType.VIDEO, BotMessageType.DOCUMENT, BotMessageType.ANIMATION
                    )
                ) {
                    if (msg.originalCaption == null)
                        msg.originalCaption = msg.caption
                    msg.caption = it

                    if(msg.user==null) {
                        val session = msg.session
                        val userId = session.user.id.toString()
                        SupportTelegramBot.findBotById(session.botId)?.let { bot ->
                            val send = EditMessageCaption()
                            send.chatId = userId
                            send.messageId = msg.messageId
                            send.caption = it
                            bot.execute(send)
                        }
                    }
                }
            }
            msg.hasRead = false
            botMessageRepository.save(msg)
    }

    override fun takeSession(id: String) {
        sessionRepository.findByHashId(id)?.let { session ->
            if (session.operatorId == null) {
                session.operatorId = getUserId()
                session.status = SessionStatusEnum.BUSY
                sessionRepository.save(session)
            } else if (session.operatorId != getUserId()) throw BusySessionException()
            else {
            }
        } ?: throw SessionNotFoundException()
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
        val fileDB = fileInfoRepository.findByHashId(hashId) ?: throw FileNotFoundException()
        val path: Path = Paths.get(fileDB.path).normalize()
        val file = path.toFile()
        response.contentType = Files.probeContentType(path) ?: "application/octet-stream"
        response.setHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        response.setContentLengthLong(file.length())

        FileInputStream(file).use { inputStream ->
            response.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
            }
        }
    }

    override fun show(hashId: String, response: HttpServletResponse) {
        val fileDB = fileInfoRepository.findByHashId(hashId) ?: throw FileNotFoundException()
        val path: Path = Paths.get(fileDB.path).normalize()
        val file = path.toFile()
        response.contentType = Files.probeContentType(path) ?: "application/octet-stream"
        response.setHeader("Content-Disposition", "inline; filename=\"${file.name}\"")
        response.setContentLengthLong(file.length())

        FileInputStream(file).use { inputStream ->
            response.outputStream.use { outputStream ->
                inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
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
        return "${FilenameUtils.removeExtension(multipartFile.originalFilename)}-${Date().time}.${
            FilenameUtils.getExtension(
                multipartFile.originalFilename
            )
        }"
    }
}

@Service
class StandardAnswerServiceImpl(
    private val repository: StandardAnswerRepository,
) : StandardAnswerService {

    override fun create(request: StandardAnswerRequest): StandardAnswerResponse {
        existsByText(request.text)
        return StandardAnswerResponse.toResponse(
            repository.save(StandardAnswerRequest.toEntity(request))
        )
    }

    override fun update(request: StandardAnswerUpdateRequest, id: Long): StandardAnswerResponse {
        request.text?.let { existsByText(id, it) }
        val answer = repository.findByIdAndDeletedFalse(id) ?: throw StandardAnswerNotFoundException()
        return StandardAnswerResponse.toResponse(repository.save(StandardAnswerUpdateRequest.toEntity(request,answer)))
    }

    override fun find(id: Long): StandardAnswerResponse {
        val answer = repository.findByIdAndDeletedFalse(id) ?: throw StandardAnswerNotFoundException()
        return StandardAnswerResponse.toResponse(answer)
    }

    override fun findAll(pageable: Pageable): Page<StandardAnswerResponse> {
        return repository.findAll(pageable).map { StandardAnswerResponse.toResponse(it) }
    }

    override fun delete(id: Long) {
        val answer = repository.findByIdAndDeletedFalse(id) ?: throw StandardAnswerNotFoundException()
        repository.delete(answer)
    }

    private fun existsByText(text: String) {
        repository.existsByText(text).takeIf { it }?.let { throw StandardAnswerAlreadyExistsException() }
    }

    private fun existsByText(id: Long, text: String) {
        repository.existsByTextAndIdNot(text,id).takeIf { it }
            ?.let { throw StandardAnswerAlreadyExistsException() }
    }
}

@Service
class StatisticServiceImpl(private val sessionRepository: SessionRepository) : StatisticService {

    override fun getSessionByOperatorDateRange(
        operatorId: Long?, startDate: Date, endDate: Date
    ): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorIdDateRange(
            startDate, endDate, getUserId()
        )
        return sessionRepository.findSessionInfoByOperatorIdDateRange(startDate, endDate, operatorId)
    }

    override fun getSessionByOperatorDateRange(operatorId: Long?, date: Date): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorIdAndDate(date, getUserId())
        return sessionRepository.findSessionInfoByOperatorIdAndDate(date, operatorId)
    }

    override fun getSessionByOperatorDateRange(operatorId: Long?): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorId(getUserId())
        return sessionRepository.findSessionInfoByOperatorId(operatorId)
    }


}