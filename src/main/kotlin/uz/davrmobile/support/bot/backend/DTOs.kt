package uz.davrmobile.support.bot.backend

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import uz.davrmobile.support.bot.bot.Utils.Companion.randomHashId
import uz.davrmobile.support.entity.BaseEntity
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity

data class BaseMessage(val code: Int, val message: String?)

data class AddOperatorRequest(
    val userId: Long,
    val userRole: UserRole,
    val languages: MutableSet<LanguageEnum>
)

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

data class SessionInfo(
    val user: UserResponse,
    val status: SessionStatusEnum,
    val operatorId: Long?,
    val rate: Short?
)

data class RateInfo(
    val rate: Double,
    val operator: UserResponse,
)

data class DateRangeRequest(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fromDate: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val toDate: Date
)

data class TokenRequest(
    val token: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BotResponse(
    val id: String,
    var token: String?,
    val username: String,
    val name: String,
    val status: BotStatusEnum
) {
    companion object {
        fun torResponse(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(hashId, token, username, name, status)
            }
        }
    }
}

data class GetSessionsResponse(
    val myConnectedSessions: List<SessionResponse>,
    val waitingSessions: Page<SessionResponse>
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
    val botId: String,
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
                    bot.hashId,
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
                    hashId,
                    UserResponse.toResponse(user),
                    unreadMessages.map { BotMessageResponse.toResponse(it) }
                )
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
    val fileHash: String?,
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
                    file?.hashId,
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
    @Nullable val fileId: List<String>?,
    @Nullable val location: LocationRequest?,
    @Nullable val contact: ContactRequest?,
    @Nullable val dice: DiceRequest?
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double,
)

data class ContactRequest(
    val name: String,
    val phoneNumber: String,
)

data class DiceRequest(
    val value: Int,
    val emoji: String
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
    val value: Int,
    val emoji: String
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
){
    companion object{
        fun toEntity(request: StandardAnswerRequest): StandardAnswer = StandardAnswer(text = request.text)
    }
}
data class StandardAnswerUpdateRequest(
    val text: String?
){
    companion object{
        fun toEntity(request: StandardAnswerUpdateRequest, entity: StandardAnswer): StandardAnswer{
            request.run {
                text?.let {entity.text = it}
            }
            return entity
        }
    }
}
data class StandardAnswerResponse(
    val id: Long,
    val text: String
){
    companion object{
        fun toResponse(answer: StandardAnswer): StandardAnswerResponse = StandardAnswerResponse(answer.id!!, answer.text)
    }
}

data class GetSessionRequest(
   @NotNull var languages: MutableList<LanguageEnum>,
)

interface SessionInfoByOperator {
    val operatorId: Long
    val sessionCount: Int
    val messageCount: Int
    val avgRate: Double
}
