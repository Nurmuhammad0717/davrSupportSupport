package uz.davrmobile.support.bot.backend

import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot
import uz.davrmobile.support.bot.bot.Utils.Companion.isAdmin
import uz.davrmobile.support.util.userId
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional


interface UserService {
    fun getAllUsers(pageable: Pageable, fullName: String?): Page<UserResponse>
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
    fun upload(multipartFileList: MutableList<MultipartFile>): UploadFileResponse
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
    fun getSessionByOperatorDateRange(request: OperatorStatisticRequest): SessionInfoByOperatorResponse
}

interface MessageToOperatorService {
    fun getWaitingSessions(request: GetSessionRequest, pageable: Pageable): Page<SessionResponse>
    fun getMySessions(pageable: Pageable): Page<SessionResponse>
    fun getSessionMessages(id: String, pageable: Pageable): SessionMessagesResponse
    fun getUnreadMessages(id: String, pageable: Pageable): SessionMessagesResponse
    fun sendMessage(message: OperatorSentMsgRequest): BotMessageResponse
    fun closeSession(sessionHash: String)
    fun editMessage(message: OperatorEditMsgRequest)
    fun editMessage(text: String?, caption: String?, msg: BotMessage)
    fun takeSession(id: String)
    fun getOperatorBots(): GetOperatorBotsResponse
}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository, private val operatorLanguageRepository: OperatorLanguageRepository
) : UserService {
    override fun getAllUsers(pageable: Pageable, fullName: String?): Page<UserResponse> {
        return fullName?.let {
            userRepository.findAllByDeletedFalseAndFullNameContainsIgnoreCaseOrderByIdDesc(pageable, it).map { user ->
                UserResponse.toResponse(user)
            }
        } ?: userRepository.findAllByDeletedFalseOrderByIdDesc(pageable).map {
            UserResponse.toResponse(it)
        }
    }

    override fun deleteUser(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        user.deleted = true
        userRepository.save(user)
    }

    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        return UserResponse.toResponse(user)
    }

    override fun addOperatorLanguages(languages: List<LanguageEnum>) {
        val userId = userId()
        for (it in languages) {
            operatorLanguageRepository.save(OperatorLanguage(userId, it))
        }
    }

    override fun removeOperatorLanguage(id: Long) {
        operatorLanguageRepository.trash(id) ?: throw OperatorLanguageNotFoundException()
    }
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
    override fun getMySessions(pageable: Pageable): Page<SessionResponse> {
        return sessionRepository.findAllByOperatorIdAndStatus(
            userId(),
            SessionStatusEnum.BUSY,
            pageable
        ).map { sessionToResp(it) }
    }

    override fun getWaitingSessions(request: GetSessionRequest, pageable: Pageable): Page<SessionResponse> {
        if (request.languages.isEmpty())
            request.languages.addAll(LanguageEnum.values())

        return sessionRepository.getWaitingSessions(
            BotStatusEnum.ACTIVE.toString(),
            userId(),
            SessionStatusEnum.WAITING.toString(),
            request.languages.map { it.ordinal },
            pageable
        ).map { sessionToResp(it) }
    }

    private fun sessionToResp(session: Session): SessionResponse {
        val count = botMessageRepository.countAllBySessionIdAndHasReadFalseAndDeletedFalse(session.id!!)
        val bot = botRepository.findByChatIdAndStatusAndDeletedFalse(session.botId, BotStatusEnum.ACTIVE)
            ?: throw BotNotFoundException()
        val lastMsg =
            botMessageRepository.findFirstBySessionIdOrderByCreatedDateDesc(session.id!!)
        return SessionResponse.toResponse(session, count, bot, lastMsg?.let { BotMessageResponse.toResponse(lastMsg) })
    }

    override fun getSessionMessages(id: String, pageable: Pageable): SessionMessagesResponse {
        val session = sessionRepository.findByHashId(id) ?: throw SessionNotFoundException()
        val messages = botMessageRepository.findAllBySessionIdAndDeletedFalse(session.id!!, pageable)
        return SessionMessagesResponse.toResponse(session, messages)
    }

    @Transactional
    override fun getUnreadMessages(id: String, pageable: Pageable): SessionMessagesResponse {
        val session = sessionRepository.findByHashId(id) ?: throw SessionNotFoundException()
        val unreadMessages =
            botMessageRepository.findAllBySessionIdAndHasReadFalseAndDeletedFalse(session.id!!, pageable)
        for (unreadMessage in unreadMessages) unreadMessage.hasRead = true
        botMessageRepository.saveAll(unreadMessages)
        return SessionMessagesResponse.toResponse(session, unreadMessages)

    }

    @Transactional
    override fun sendMessage(message: OperatorSentMsgRequest): BotMessageResponse {
        val operatorId = userId()

        if (message.sessionId == null) throw FieldIsRequiredException(OperatorSentMsgRequest::sessionId.name)
        val session = sessionRepository.findByHashId(message.sessionId) ?: throw SessionNotFoundException()

        if (session.operatorId == null) throw SessionNotConnectedToOperatorException()
        if (session.operatorId != userId()) throw BusySessionException()

        val userId = session.user.id.toString()

        val bot = botRepository.findByChatIdAndDeletedFalse(session.botId) ?: throw BotNotFoundException()
        val absSender = SupportTelegramBot.findBotById(bot.chatId) ?: throw BotNotFoundException()

        return when (message.type!!) {
            BotMessageType.TEXT -> {
                val text = message.text ?: throw FieldIsRequiredException(OperatorSentMsgRequest::text.name)
                if (text.length > 4096) throw MaximumTextLengthException()
                if (text.isEmpty()) throw FieldCantBeEmptyException(OperatorSentMsgRequest::text.name)
                val send = SendMessage(userId, text)
                send.replyToMessageId = message.replyMessageId
                val ex = absSender.execute(send)
                BotMessageResponse
                    .toResponse(newSessionMsg(message, session, operatorId, ex.messageId))
            }

            BotMessageType.CONTACT -> {
                val it = message.contact ?: throw FieldIsRequiredException(OperatorSentMsgRequest::contact.name)
                val send = SendContact(userId, it.phoneNumber, it.name)
                send.replyToMessageId = message.replyMessageId
                val ex = absSender.execute(send)
                BotMessageResponse.toResponse(
                    newSessionMsg(
                        message, session, operatorId, ex.messageId, contact = send
                    )
                )
            }

            BotMessageType.LOCATION -> {
                val it = message.location ?: throw FieldIsRequiredException(OperatorSentMsgRequest::location.name)
                val send = SendLocation(userId, it.latitude, it.longitude)
                send.replyToMessageId = message.replyMessageId
                val ex = absSender.execute(send)
                BotMessageResponse.toResponse(
                    newSessionMsg(
                        message, session, operatorId, ex.messageId, location = send
                    )
                )
            }

            BotMessageType.ANIMATION -> {
                val fileHashIds =
                    message.fileIds ?: throw FieldIsRequiredException(OperatorSentMsgRequest::fileIds.name)
                if (fileHashIds.isEmpty()) throw FieldCantBeEmptyException(OperatorSentMsgRequest::fileIds.name)

                var lastMsg: BotMessageResponse? = null
                for (fileHashId in fileHashIds) {
                    val fileInfo = fileInfoRepository.findByHashId(fileHashId)!!
                    val send = SendAnimation()
                    send.chatId = userId
                    send.animation =
                        InputFile(File(Paths.get(fileInfo.path).toAbsolutePath().toString()))
                    if (!message.caption.isNullOrEmpty())
                        if (message.caption.length > 4096)
                            throw MaximumTextLengthException()
                        else send.caption = message.caption
                    send.replyToMessageId = message.replyMessageId
                    val ex = absSender.execute(send)
                    lastMsg = BotMessageResponse
                        .toResponse(
                            newSessionMsg(message, session, operatorId, ex.messageId, fileInfos = listOf(fileInfo))
                        )
                }
                lastMsg!!
            }

            BotMessageType.VIDEO, BotMessageType.PHOTO, BotMessageType.VOICE, BotMessageType.AUDIO, BotMessageType.DOCUMENT -> {
                val fileHashIds =
                    message.fileIds ?: throw FieldIsRequiredException(OperatorSentMsgRequest::fileIds.name)
                if (fileHashIds.isEmpty()) throw FieldCantBeEmptyException(OperatorSentMsgRequest::fileIds.name)

                val inputMediaList: MutableList<InputMedia> = mutableListOf()
                for (fileHashId in fileHashIds) {
                    val fileInfo = fileInfoRepository.findByHashId(fileHashId)!!
                    inputMediaList.add(getInputMediaByFileInfo(fileInfo, message.caption))
                }
                sendMediaGroup(userId, inputMediaList, absSender, message, session, operatorId)
            }

            else -> throw UnSupportedMessageTypeException()
        }
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
    ): BotMessageResponse {
        var isDocument = false
        val caption = message.caption
        val replyMessageId = message.replyMessageId
        for (inputMedia in inputMediaList) if (inputMedia is InputMediaDocument) isDocument = true
        var inputMediaListTemp: MutableList<InputMedia> = mutableListOf()
        if (isDocument) {
            for ((index, inputMedia) in inputMediaList.withIndex()) {
                val send = InputMediaDocument()
                send.setMedia(inputMedia.newMediaFile, inputMedia.mediaName)
                if (inputMediaList.size - 1 == index && !caption.isNullOrEmpty())
                    if (message.caption.length > 4096)
                        throw MaximumTextLengthException()
                    else send.caption = message.caption
                inputMediaListTemp.add(send)
            }
        } else inputMediaListTemp = inputMediaList

        if (inputMediaListTemp.size == 1) {
            val media = inputMediaListTemp[0]
            val inputFile = InputFile().apply { setMedia(media.newMediaFile, media.mediaName) }


            val ex = when (media) {
                is InputMediaAnimation -> {
                    absSender.execute(SendAnimation().apply {
                        chatId = userId
                        animation = inputFile
                        if (!caption.isNullOrEmpty())
                            if (message.caption.length > 4096)
                                throw MaximumTextLengthException()
                            else this.caption = caption
                        replyToMessageId = replyMessageId
                    })
                }

                is InputMediaVideo -> {
                    absSender.execute(SendVideo().apply {
                        chatId = userId
                        video = inputFile
                        if (!caption.isNullOrEmpty())
                            if (message.caption.length > 4096)
                                throw MaximumTextLengthException()
                            else this.caption = caption
                        replyToMessageId = replyMessageId
                    })
                }

                is InputMediaPhoto -> {
                    absSender.execute(SendPhoto(userId, inputFile).apply {
                        chatId = userId
                        photo = inputFile
                        if (!caption.isNullOrEmpty())
                            if (message.caption.length > 4096)
                                throw MaximumTextLengthException()
                            else this.caption = caption
                        replyToMessageId = replyMessageId
                    })
                }

                is InputMediaAudio -> {
                    absSender.execute(SendAudio().apply {
                        chatId = userId
                        audio = inputFile
                        if (!caption.isNullOrEmpty())
                            if (message.caption.length > 4096)
                                throw MaximumTextLengthException()
                            else this.caption = caption
                        replyToMessageId = replyMessageId
                    })
                }

                else -> {
                    absSender.execute(SendDocument().apply {
                        chatId = userId
                        document = inputFile
                        if (!caption.isNullOrEmpty())
                            if (message.caption.length > 4096)
                                throw MaximumTextLengthException()
                            else this.caption = caption
                        replyToMessageId = replyMessageId
                    })
                }
            }
            return BotMessageResponse.toResponse(
                newSessionMsg(
                    message,
                    session,
                    operatorId,
                    ex.messageId,
                    fileInfos = message.fileIds?.let { fileInfoRepository.findAllByHashIdIn(it) })
            )
        } else if (inputMediaListTemp.size > 10) {
            var lastMsg: BotMessageResponse? = null
            inputMediaListTemp.chunked(10).map { inputMedia ->
                lastMsg = sendMediaGroup(userId, inputMedia.toMutableList(), absSender, message, session, operatorId)
            }
            return lastMsg!!
        } else {
            val send = SendMediaGroup(userId, inputMediaListTemp)
            send.protectContent = true
            send.replyToMessageId = replyMessageId
            val exs = absSender.execute(send)
            var lastMsg: BotMessage? = null
            for (ex in exs) {
                lastMsg = newSessionMsg(
                    message,
                    session,
                    operatorId,
                    ex.messageId,
                    fileInfos = message.fileIds?.let { fileInfoRepository.findAllByHashIdIn(it) })
            }
            return BotMessageResponse.toResponse(lastMsg!!)
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
        if (!caption.isNullOrEmpty())
            inputMedia.caption = caption
        return inputMedia
    }

    override fun closeSession(sessionHash: String) {
        sessionRepository.findByHashId(sessionHash)?.let {
            if (it.isClosed()) return
            it.status = SessionStatusEnum.CLOSED
            sessionRepository.save(it)
            SupportTelegramBot.findBotById(it.botId)?.stopChat(it)
        }
    }

    override fun editMessage(message: OperatorEditMsgRequest) {
        val msg = botMessageRepository.findByMessageIdAndDeletedFalse(message.messageId!!.toInt())
            ?: throw MessageNotFoundException()
        editMessage(message.text, message.caption, msg)
    }


    override fun editMessage(text: String?, caption: String?, msg: BotMessage) {
        text?.let {
            if (msg.botMessageType == BotMessageType.TEXT) {
                if (msg.originalText == null) msg.originalText = msg.text
                msg.text = it

                if (msg.user == null) {
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
                msg.hasRead = false
            }
        }

        caption?.let {
            if (it.isNotEmpty()) {
                if (msg.botMessageType in listOf(
                        BotMessageType.PHOTO, BotMessageType.VIDEO, BotMessageType.DOCUMENT, BotMessageType.ANIMATION
                    )
                ) {
                    if (msg.originalCaption == null) msg.originalCaption = msg.caption
                    msg.caption = it

                    if (msg.user == null) {
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
                    msg.hasRead = false
                }
            }
        }
        botMessageRepository.save(msg)
    }

    override fun takeSession(id: String) {
        sessionRepository.findByHashId(id)?.let { session ->
            if (session.operatorId == null) {
                session.operatorId = userId()
                session.status = SessionStatusEnum.BUSY
                sessionRepository.save(session)
            } else if (session.operatorId != userId()) throw BusySessionException()
            else {
            }
        } ?: throw SessionNotFoundException()
    }

    override fun getOperatorBots(): GetOperatorBotsResponse {
        return GetOperatorBotsResponse(botRepository.findAllBotsByOperatorIdsContains(userId()).map {
            BotResponse.toResponseWithoutToken(it)
        })
    }
}

@Service
class FileInfoServiceImpl(private val fileInfoRepository: FileInfoRepository) : FileInfoService {

    private val path: String = "files/${LocalDate.now()}"

    override fun upload(multipartFileList: MutableList<MultipartFile>): UploadFileResponse {
        val responseFiles: MutableList<FileInfoResponse> = mutableListOf()
        multipartFileList.forEach { multipartFile ->
            val name = takeFileName(multipartFile)
            var width: Int? = null
            var height: Int? = null
            try {
                ImageIO.read(multipartFile.inputStream)?.let {
                    width = it.width
                    height = it.height
                }
            } catch (_: Exception) {
            }

            val fileInfo = FileInfo(
                name = name,
                uploadName = multipartFile.originalFilename!!,
                extension = FilenameUtils.getExtension(multipartFile.originalFilename),
                path = getFilePath(name).toString(),
                size = multipartFile.size,
                width = width,
                height = height
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
        return UploadFileResponse(responseFiles)
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
        return StandardAnswerResponse.toResponse(repository.save(StandardAnswerUpdateRequest.toEntity(request, answer)))
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
        repository.existsByTextAndIdNot(text, id).takeIf { it }?.let { throw StandardAnswerAlreadyExistsException() }
    }
}

@Service
class StatisticServiceImpl(private val sessionRepository: SessionRepository) : StatisticService {
    override fun getSessionByOperatorDateRange(
        operatorId: Long?, startDate: Date, endDate: Date
    ): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorIdDateRange(
            startDate,
            endDate,
            userId()
        ) ?: throw InformationNotFoundException()
        if (isAdmin()) {
            return sessionRepository.findSessionInfoByOperatorIdDateRange(startDate, endDate, operatorId)
                ?: throw InformationNotFoundException()
        } else throw NoAuthorityException()
    }

    override fun getSessionByOperatorDateRange(operatorId: Long?, date: Date): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorIdAndDate(date, userId())
            ?: throw InformationNotFoundException()
        if (isAdmin()) {
            return sessionRepository.findSessionInfoByOperatorIdAndDate(date, operatorId)
                ?: throw InformationNotFoundException()
        } else throw NoAuthorityException()
    }

    override fun getSessionByOperatorDateRange(operatorId: Long?): SessionInfoByOperatorResponse {
        if (operatorId == null) return sessionRepository.findSessionInfoByOperatorId(userId())
            ?: throw InformationNotFoundException()
        if (isAdmin()) {
            return sessionRepository.findSessionInfoByOperatorId(operatorId) ?: throw InformationNotFoundException()
        } else throw NoAuthorityException()
    }

    override fun getSessionByOperatorDateRange(request: OperatorStatisticRequest): SessionInfoByOperatorResponse {
        request.run {
            return if (startDate != null && endDate != null) {
                getSessionByOperatorDateRange(operatorId, startDate, endDate)
            } else if (startDate != null) {
                getSessionByOperatorDateRange(operatorId, startDate)
            } else {
                getSessionByOperatorDateRange(operatorId)
            }
        }
    }
}