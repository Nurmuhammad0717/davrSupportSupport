package uz.davrmobile.support.exception

import org.springframework.stereotype.Service

@Service
class DeveloperLoggerBotImpl : DeveloperLoggerBot {

//    private val log = KotlinLogging.logger { }

    @Suppress("TooGenericExceptionCaught")
    override fun sendError(error: BotErrorModel) {
//        try {
//            restTemplate.exchange("$url/error", HttpMethod.POST, HttpEntity(error), Any::class.java)
//        } catch (ex: Exception) {
//            log.warn(ex) { "Failed while sending 'error' to developer's bot" }
//        }
    }
}
