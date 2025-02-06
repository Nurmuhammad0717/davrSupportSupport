package uz.davrmobile.support.bot.backend

import com.fasterxml.jackson.annotation.JsonInclude
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.springframework.data.domain.Page
import uz.davrmobile.support.util.userId
import java.util.*

data class BaseMessage(val code: Int, val message: String?)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val language: Set<LanguageEnum>,
    val role: UserRole?,
    val miniPhotoId: String?,
    val bigPhotoId: String?,
) {
    companion object {
        fun toResponse(botUser: BotUser): UserResponse {
            botUser.run {
                return UserResponse(id, username, fullName, phoneNumber, languages, role, miniPhotoId, bigPhotoId)
            }
        }
    }
}

data class TokenRequest(
    val token: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BotResponse(
    val id: String,
    val username: String,
    val name: String,
    val status: BotStatusEnum,
    var token: String?,
    val miniPhotoId: String?,
    val bigPhotoId: String?,
    val isJoined: Boolean,
) {
    companion object {
        fun toResponse(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(
                    hashId, username, name, status, token, miniPhotoId, bigPhotoId, bot.operatorIds.contains(userId())
                )
            }
        }

        fun toResponseWithoutToken(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(
                    hashId, username, name, status, null, miniPhotoId, bigPhotoId, bot.operatorIds.contains(userId())
                )
            }
        }
    }
}

data class GetSessionsResponse(
    val sessions: List<SessionResponse>
)


@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserSessionResponse(
    val id: Long,
    val fullName: String,
    val miniPhotoId: String?,
    val bigPhotoId: String?,
) {
    companion object {
        fun toResponse(user: BotUser): UserSessionResponse {
            user.run {
                return UserSessionResponse(id, fullName, miniPhotoId, bigPhotoId)
            }
        }
    }
}

data class SessionResponse(
    val id: String,
    val user: UserSessionResponse,
    val bot: BotResponse,
    val status: SessionStatusEnum,
    val newMessagesCount: Int,
    val language: LanguageEnum,
    val date: Long,
    var lastMessage: BotMessageResponse? = null,
) {
    companion object {
        fun toResponse(
            session: Session, messageCount: Int, bot: Bot, lastMessage: BotMessageResponse?
        ): SessionResponse {
            session.run {
                return SessionResponse(
                    hashId,
                    UserSessionResponse.toResponse(user),
                    BotResponse.toResponseWithoutToken(bot),
                    status!!,
                    messageCount,
                    language,
                    createdDate!!.toInstant().toEpochMilli(),
                    lastMessage
                )
            }
        }
    }
}

data class SessionMessagesResponse(
    val sessionId: String, val from: UserResponse, val messages: Page<BotMessageResponse>
) {
    companion object {
        fun toResponse(session: Session, unreadMessages: Page<BotMessage>): SessionMessagesResponse {
            return session.run {
                SessionMessagesResponse(
                    hashId, UserResponse.toResponse(user), unreadMessages.map { BotMessageResponse.toResponse(it) })
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BotMessageResponse(
    val id: String,
    val messageId: Int,
    val type: BotMessageType,
    val replyMessageId: Int?,
    val text: String?,
    val caption: String?,
    val date: Long,
    val files: List<FileInfoResponse>?,
    val location: LocationResponse?,
    val contact: ContactResponse?,
    val dice: DiceResponse?,
    var edited: Boolean = false,
    var from: Long
) {
    companion object {
        fun toResponse(botMessage: BotMessage): BotMessageResponse {
            botMessage.run {
                return BotMessageResponse(
                    hashId,
                    messageId,
                    botMessageType,
                    replyMessageId,
                    text,
                    caption,
                    createdDate!!.toInstant().toEpochMilli(),
                    files?.let {
                        if (it.isNotEmpty()) it.map { u -> FileInfoResponse.toResponse(u) }
                        else null
                    },
                    location?.let { LocationResponse.toResponse(it) },
                    contact?.let { ContactResponse.toResponse(it) },
                    dice?.let { DiceResponse.toResponse(it) },
                    (botMessage.originalText != null || botMessage.originalCaption != null),
                    botMessage.fromOperatorId ?: botMessage.user!!.id
                )

            }
        }
    }
}

data class OperatorSentMsgRequest(
    val sessionId: String?,
    val type: BotMessageType?,
    @Nullable val replyMessageId: Int?,
    @Nullable val text: String?,
    @Nullable val caption: String?,
    @Nullable val fileIds: List<String>?,
    @Nullable val location: LocationRequest?,
    @Nullable val contact: ContactRequest?,
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double,
)

data class ContactRequest(
    val name: String,
    val phoneNumber: String,
)

data class LocationResponse(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun toResponse(location: Location): LocationResponse {
            location.run {
                return LocationResponse(latitude, longitude)
            }
        }
    }
}

data class ContactResponse(
    val name: String,
    val phoneNumber: String,
) {
    companion object {
        fun toResponse(contact: Contact): ContactResponse {
            contact.run {
                return ContactResponse(name, phoneNumber)
            }
        }
    }
}

data class DiceResponse(
    val value: Int, val emoji: String
) {
    companion object {
        fun toResponse(dice: Dice): DiceResponse {
            dice.run {
                return DiceResponse(value, value.toString())
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FileInfoResponse(
    val upload: String,
    val name: String,
    val hashId: String,
    val extension: String,
    val size: Long,
    val width: Int?,
    val height: Int?,
) {
    companion object {
        fun toResponse(file: FileInfo): FileInfoResponse = file.run {
            FileInfoResponse(uploadName, name, hashId, extension, size, width, height)
        }
    }
}

data class StandardAnswerRequest(
    val text: String
) {
    companion object {
        fun toEntity(request: StandardAnswerRequest): StandardAnswer = StandardAnswer(text = request.text)
    }
}

data class StandardAnswerUpdateRequest(
    val text: String?
) {
    companion object {
        fun toEntity(request: StandardAnswerUpdateRequest, entity: StandardAnswer): StandardAnswer {
            request.run {
                text?.let { entity.text = it }
            }
            return entity
        }
    }
}

data class StandardAnswerResponse(
    val id: Long, val text: String
) {
    companion object {
        fun toResponse(answer: StandardAnswer): StandardAnswerResponse =
            StandardAnswerResponse(answer.id!!, answer.text)
    }
}

data class GetSessionRequest(
    @NotNull var languages: MutableList<LanguageEnum>,
)

interface SessionInfoByOperatorResponse {
    var operatorId: Long?
    val sessionCount: Int?
    val messageCount: Int?
    val avgRate: Double?
}

data class OperatorEditMsgRequest(
    val sessionId: String?,
    val type: BotMessageType?,
    val messageId: Long?,
    @Nullable val text: String?,
    @Nullable val caption: String?,
)

data class OperatorStatisticRequest(
    val operatorId: Long?, val startDate: Date?, val endDate: Date?
)

data class SavedTgFileResponse(
    val name: String,
    val extension: String,
    val path: String,
    val size: Long,
    val width: Int?,
    val height: Int?,
) {
    fun toEntity(): FileInfo {
        return FileInfo(name, name, extension, path, size, width, height)
    }
}

data class GetOperatorBotsResponse(
    val bots: List<BotResponse>
)

data class UploadFileResponse(
    val files: List<FileInfoResponse>
)

interface LastMessageWithCount {
    val lastMessage: BotMessage?
    val unreadMessageCount: Int
    val bot: Bot?
}

data class
ClosedSessionResponse(
    val id: String,
    val rate: Int?,
    val language: String,
    val date: Long,
    val status: String,
    val userId: Long,
    val userFullName: String?,
    val botId: String,
    val botUsername: String,
    val botName: String,
) {
    companion object {
        fun toResponse(p: ClosedSessionProjection): ClosedSessionResponse {
            return ClosedSessionResponse(
                p.getId(),
                p.getRate(),
                p.getLanguage(),
                p.getDate().toInstant().toEpochMilli(),
                p.getStatus(),
                p.getUserId(),
                p.getUserFullName(),
                p.getBotId(),
                p.getBotUsername(),
                p.getBotName(),
            )
        }
    }
}

interface ClosedSessionProjection {
    fun getId(): String
    fun getRate(): Int?
    fun getLanguage(): String
    fun getDate(): Date
    fun getStatus(): String
    fun getUserId(): Long
    fun getUserFullName(): String?
    fun getBotId(): String
    fun getBotUsername(): String
    fun getBotName(): String
}
