package uz.davrmobile.support.enm

@Suppress("MagicNumber")
enum class ErrorCode(val code: Int) {
    INTERNAL_SERVER_ERROR(600),
    CHAT_NOT_FOUND(621),
    SECTION_NOT_SELECTED(623),
    CHAT_IS_OPEN(624),
    CHAT_IS_NOT_OPEN(625),
    ILLEGAL_OPERATION(626),
    NOT_FOUND_EXCEPTION(627),
}
