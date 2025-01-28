package uz.davrmobile.support.bot.backend

import com.fasterxml.jackson.annotation.JsonInclude
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.springframework.data.domain.Page
import java.util.*

data class BaseMessage(val code: Int, val message: String?)

data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val language: Set<LanguageEnum>,
    val role: UserRole?
) {
    companion object {
        fun toResponse(botUser: BotUser): UserResponse {
            botUser.run {
                return UserResponse(id, username, fullName, phoneNumber, languages, role)
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
) {
    companion object {
        fun toResponse(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(hashId, username, name, status, token, miniPhotoId, bigPhotoId)
            }
        }

        fun toResponseWithoutToken(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(hashId, username, name, status, null, miniPhotoId, bigPhotoId)
            }
        }
    }
}

data class GetSessionsResponse(
    val myConnectedSessions: List<SessionResponse>, val waitingSessions: Page<SessionResponse>
)


data class UserSessionResponse(
    val id: Long,
    val fullName: String,
) {
    companion object {
        fun toResponse(user: BotUser): UserSessionResponse {
            return UserSessionResponse(user.id, user.fullName)
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
    val date: Long
) {
    companion object {
        fun toResponse(session: Session, messageCount: Int, bot: Bot): SessionResponse {
            session.run {
                return SessionResponse(
                    hashId,
                    UserSessionResponse.toResponse(user),
                    BotResponse.toResponseWithoutToken(bot),
                    status!!,
                    messageCount,
                    language,
                    createdDate!!.toInstant().epochSecond
                )
            }
        }
    }
}

data class SessionMessagesResponse(
    val sessionId: String,
    val from: UserResponse,
    val messages: List<BotMessageResponse>
) {
    companion object {
        fun toResponse(session: Session, unreadMessages: List<BotMessage>): SessionMessagesResponse {
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
    val fileHashIds: List<String>?,
    val location: LocationResponse?,
    val contact: ContactResponse?,
    val dice: DiceResponse?,
    var edited: Boolean = false
) {
    companion object {
        fun toResponse(botMessage: BotMessage): BotMessageResponse {
            botMessage.run {
                return BotMessageResponse(
                    hashId,
                    messageId, botMessageType,
                    replyMessageId, text, caption,
                    createdDate!!.toInstant().epochSecond,
                    files?.let {
                        if (it.isNotEmpty()) it.map { u -> u.hashId }
                        else null
                    },
                    location?.let { LocationResponse.toResponse(it) },
                    contact?.let { ContactResponse.toResponse(it) },
                    dice?.let { DiceResponse.toResponse(it) },
                    (botMessage.originalText != null || botMessage.originalCaption != null),
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

data class FileInfoResponse(
    val id: Long,
    val upload: String,
    val name: String,
    val hashId: String,
    val extension: String,
    val size: Long,
) {
    companion object {
        fun toResponse(file: FileInfo): FileInfoResponse = FileInfoResponse(
            file.id!!, file.uploadName, file.name, file.hashId, file.extension, file.size
        )
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OperatorStatisticRequest(
    @Nullable val operatorId: Long?,
    @Nullable val startDate: Date?,
    @Nullable val endDate: Date?
)

data class SavedTgFileResponse(
    val name: String,
    val extension: String,
    val path: String,
    val size: Long,
) {
    fun toEntity(): FileInfo {
        return FileInfo(name, name, extension, path, size)
    }
}

data class GetOperatorBotsResponse(
    val bots: List<BotResponse>
)

data class UploadFileResponse(
    val files: List<FileInfoResponse>
)

data class GetAllUsersResponse(
    val users: Page<UserResponse>
)