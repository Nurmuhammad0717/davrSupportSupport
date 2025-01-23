package uz.davrmobile.support.bot.backend

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.jetbrains.annotations.Nullable
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

data class BotResponse(
    val id: String,
    val token: String,
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

data class SessionResponse(
    val id: String,
    val user: UserResponse,
    val botId: String,
    val status: SessionStatusEnum,
    val newMessagesCount: Int
) {
    companion object {
        fun toResponse(session: Session, messageCount: Int, bot: Bot): SessionResponse {
            session.run {
                return SessionResponse(hashId, UserResponse.toResponse(user), bot.hashId, status!!, messageCount)
            }
        }
    }
}

data class SessionMessagesResponse(
    val sessionId: String,
    val from: UserResponse,
    val messages: List<BotMessageResponse>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BotMessageResponse(
    val id: String,
    val messageId: Int,
    val type: BotMessageType,
    val replyMessageId: Int?,
    val text: String?,
    val caption: String?,
    val fileHash: String?,
    val location: LocationResponse?,
    val contact: ContactResponse?,
    val dice: DiceResponse?
) {
    companion object {
        fun toResponse(botMessage: BotMessage): BotMessageResponse {
            botMessage.run {
                return BotMessageResponse(
                    hashId,
                    messageId, botMessageType,
                    replyMessageId, text, caption,
                    file?.hashId,
                    location?.let { LocationResponse.toResponse(it) },
                    contact?.let { ContactResponse.toResponse(it) },
                    dice?.let { DiceResponse.toResponse(it) }
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
    var name: String,
    val extension: String,
    val size: Long,
    val hashId: String
) {
    companion object {
        fun toResponse(fileInfo: FileInfo): FileInfoResponse {
            fileInfo.run {
                return FileInfoResponse(name, extension, size, hashId)
            }
        }
    }
}