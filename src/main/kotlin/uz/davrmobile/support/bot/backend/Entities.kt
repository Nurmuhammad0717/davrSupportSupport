package uz.davrmobile.support.bot.backend

import javax.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uz.davrmobile.support.bot.bot.Utils.Companion.randomHashId
import uz.davrmobile.support.entity.BaseEntity
import java.util.*

//@MappedSuperclass
//@EntityListeners(AuditingEntityListener::class)
//class BaseEntity(
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
//    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
//    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
//    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
//)

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseUserEntity(
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity(name = "bot_user")
class BotUser(
    @Id @Column(nullable = false) var id: Long,
    @Column(nullable = false, length = 64) val username: String,
    @Column(nullable = false, length = 124) var fullName: String,
    @Column(nullable = false, length = 13) var phoneNumber: String,
    @Column(nullable = false) var botId: Long,
    @ElementCollection(targetClass = LanguageEnum::class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_language", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    var languages: MutableSet<LanguageEnum> = mutableSetOf(),
    @Enumerated(value = EnumType.STRING) var state: UserStateEnum = UserStateEnum.NEW_USER,
    @Enumerated(value = EnumType.STRING) var role: UserRole? = UserRole.USER,

    var msgIdChooseLanguage: Int? = null,
    val hashId: String = randomHashId()
) : BaseUserEntity() {
    fun isUser(): Boolean {
        return role == UserRole.USER
    }

    fun isTalking(): Boolean {
        return state == UserStateEnum.TALKING
    }
}

@Table(
    indexes = [
        Index(columnList = "user_id, operator_id")
    ]
)
@Entity
class Session(
    @ManyToOne @JoinColumn(name = "user_id") val user: BotUser,
    val botId: Long,
    @Enumerated(EnumType.STRING) var status: SessionStatusEnum? = SessionStatusEnum.WAITING,
    @Column(name = "operator_id") var operatorId: Long? = null,
    var rate: Short? = null,
    val hashId: String = randomHashId()
) : BaseEntity() {
    fun hasOperator(): Boolean {
        return operatorId != null
    }

    fun isClosed(): Boolean {
        return status == SessionStatusEnum.CLOSED
    }

    fun isBusy(): Boolean {
        return status == SessionStatusEnum.BUSY
    }

    fun isWaiting(): Boolean {
        return status == SessionStatusEnum.WAITING
    }
}

@Entity(name = "location")
class Location(
    @Column(nullable = false) val latitude: Double,
    @Column(nullable = false) val longitude: Double,
) : BaseEntity()

@Entity(name = "dice")
class Dice(
    @Column(nullable = false) val value: Int,
    @Column(nullable = false) val emoji: String,
) : BaseEntity()


@Table(
    indexes = [
        Index(columnList = "id")
    ]
)
@Entity
class Bot(
    @Column(nullable = false) val token: String,
    @Column(nullable = false) val username: String,
    val name: String,
    @Enumerated(value = EnumType.STRING) var status: BotStatusEnum = BotStatusEnum.ACTIVE,
    @ElementCollection var operatorIds: MutableSet<Long> = mutableSetOf(),
    val hashId: String = randomHashId()
) : BaseEntity()

@Table(
    indexes = [
        Index(columnList = "user_id, session_id")
    ]
)
@Entity(name = "bot_message")
class BotMessage(
    @ManyToOne @JoinColumn(name = "session_id") val session: Session,
    @Column(nullable = false) val messageId: Int,
    @Column(nullable = true) val replyMessageId: Int? = null,
    @Column(nullable = true, columnDefinition = "TEXT") var text: String? = null,
    @Column(nullable = true, columnDefinition = "TEXT") var originalText: String? = null,
    @Column(nullable = true, columnDefinition = "TEXT") var caption: String? = null,
    @Column(nullable = true, columnDefinition = "TEXT") var originalCaption: String? = null,
    @Enumerated(value = EnumType.STRING) val botMessageType: BotMessageType,
    @OneToOne @JoinColumn(nullable = true) val file: FileInfo? = null,
    @OneToOne @JoinColumn(nullable = true) val location: Location? = null,
    @OneToOne @JoinColumn(nullable = true) val contact: Contact? = null,
    @OneToOne @JoinColumn(nullable = true) val dice: Dice? = null,
    var hasRead: Boolean = false,
    @ManyToOne @JoinColumn(name = "user_id") val user: BotUser? = null,
    val fromOperatorId: Long? = null,
    val hashId: String = randomHashId()
) : BaseEntity()

@Entity
class Contact(
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val phoneNumber: String,
) : BaseEntity()

@Entity
class FileInfo(
    @Column(nullable = false) var name: String,
    @Column(nullable = false) val uploadName: String,
    @Column(nullable = false) val extension: String,
    @Column(nullable = false) val path: String,
    @Column(nullable = false) val size: Long,
    @Column(nullable = false, unique = true) val hashId: String = randomHashId(),
) : BaseEntity()

@Entity
class OperatorLanguage(
    val operatorId: Long,
    @Enumerated(EnumType.STRING) val language: LanguageEnum
): BaseEntity()