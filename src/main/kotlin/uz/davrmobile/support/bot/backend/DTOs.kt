package uz.davrmobile.support.bot.backend

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

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
    val id: Long,
    val token: String,
    val username: String,
    val name: String,
    val status: BotStatusEnum
) {
    companion object {
        fun torResponse(bot: Bot): BotResponse {
            return bot.run {
                BotResponse(id!!, token, username, name, status)
            }
        }
    }
}

data class SessionResponse(
    val id: Long,
    val user: BotUser,
    val botId: Long,
    val status: SessionStatusEnum,
    val newMessagesCount: Int
) {
    companion object {
        fun toResponse(session: Session, messageCount: Int): SessionResponse {
            session.run {
                return SessionResponse(id!!, user, botId, status!!, messageCount)
            }
        }
    }
}

data class SessionMessagesResponse(
    val sessionId: Long,
    val messages: List<BotMessageResponse>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BotMessageResponse(
    val id: Long,
    val from: UserResponse,
    val sessionId: Long,
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
                return BotMessageResponse(id!!,
                    UserResponse.toResponse(user),
                    session.id!!, messageId, botMessageType,
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

data class LocationResponse(
    val latitude: Float,
    val longitude: Float,
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