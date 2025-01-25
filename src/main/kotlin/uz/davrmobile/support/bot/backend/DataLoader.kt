package uz.davrmobile.support.bot.backend

import org.springframework.boot.CommandLineRunner
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import uz.davrmobile.support.bot.bot.BotService
import uz.davrmobile.support.bot.bot.SupportTelegramBot
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.activeBots

@Component
class DataLoader(
    private val userRepository: UserRepository,
    private val botRepository: BotRepository,
    private val botMessageRepository: BotMessageRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository,
    private val diceRepository: DiceRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: MessageSource,
    private val botService: BotService,
    private val messageToOperatorServiceImpl: MessageToOperatorServiceImpl,
    private val fileInfoRepository: FileInfoRepository,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val dir = java.io.File("./files")
        if (!dir.exists()) {
            dir.mkdirs()
        }


        val bots = botRepository.findAllByStatus(BotStatusEnum.ACTIVE)
        for (bot in bots) {
            val supportTelegramBot = SupportTelegramBot(
                "",
                bot.token,
                1L,
                userRepository,
                botMessageRepository,
                locationRepository,
                contactRepository,
                diceRepository,
                sessionRepository,
                messageSource,
                fileInfoRepository,
                botRepository,
                messageToOperatorServiceImpl
            )
            val me = supportTelegramBot.meAsync.get()
            supportTelegramBot.botId = bot.id!!
            supportTelegramBot.username = me.userName

            botService.registerBot(supportTelegramBot)
            activeBots[bot.token] = supportTelegramBot
        }
    }
}