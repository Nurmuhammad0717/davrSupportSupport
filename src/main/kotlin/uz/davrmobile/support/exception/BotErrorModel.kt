package uz.davrmobile.support.exception

data class BotErrorModel(
    val code: Long,
    val service: String,
    val message: String,
)
