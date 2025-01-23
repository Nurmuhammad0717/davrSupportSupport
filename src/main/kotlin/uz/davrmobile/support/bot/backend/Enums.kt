package uz.davrmobile.support.bot.backend

enum class ErrorCode(val code: Int) {
    USER_NOT_FOUND(630),
    SOMETHING_WENT_WRONG(631),
    USER_ALREADY_EXISTS(632),
    SESSION_NOT_FOUND(633),
    SESSION_ALREADY_BUSY(634),
    SESSION_CLOSED(635),
    MESSAGE_NOT_FOUND(636),
    UN_SUPPORTED_MESSAGE_TYPE(637),
    NO_SESSION_IN_QUEUE(638),
    BOT_NOT_FOUND(639),
    BAD_CREDENTIALS(640),
    ACCESS_DENIED(641),
    USERNAME_ALREADY_EXISTS(642),
    BUSY_SESSION(643)
}

enum class UserRole {
    ADMIN,
    USER,
    OPERATOR
}

enum class OperatorStatus {
    ACTIVE,
    INACTIVE,
    BUSY,
    PAUSED
}

enum class LanguageEnum {
    UZ,
    RU,
    EN
}

enum class BotMessageType {
    TEXT,
    PHOTO,
    VIDEO,
    VIDEO_NOTE,
    LOCATION,
    CONTACT,
    DICE,
    STICKER,
    AUDIO,
    ANIMATION,
    DOCUMENT,
    VOICE,
    POLL
}

enum class UserStateEnum {
    NEW_USER,
    SEND_PHONE_NUMBER,
    SEND_FULL_NAME,
    CHOOSE_LANG,
    ACTIVE_USER,
    ASK_YOUR_QUESTION,
    TALKING,
    WAITING_OPERATOR
}

enum class SessionStatusEnum {
    WAITING,
    BUSY,
    CLOSED
}

enum class BotStatusEnum {
    STOPPED,
    ACTIVE,
}
enum class MessageEffects(val id: String) {
    FIRE("5104841245755180586"),
    LIKE("5107584321108051014"),
    DIS_LIKE("5104858069142078462"),
    HEART("5044134455711629726"),
    CONFETTI("5046509860389126442")
}