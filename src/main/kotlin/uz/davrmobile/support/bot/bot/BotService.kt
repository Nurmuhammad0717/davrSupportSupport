package uz.davrmobile.support.bot.bot

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import uz.davrmobile.support.bot.backend.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.activeBots
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.findBotById


@Service
class BotService(
    private val userRepository: UserRepository,
    private val botMessageRepository: BotMessageRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository,
    private val diceRepository: DiceRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: MessageSource,
    private val botRepository: BotRepository,
    private val fileInfoRepository: FileInfoRepository,
) {

    fun createBot(req: TokenRequest) {
        val supportTelegramBot = SupportTelegramBot(
            "",
            req.token,
            -1L,
            userRepository,
            botMessageRepository,
            locationRepository,
            contactRepository,
            diceRepository,
            sessionRepository,
            messageSource,
            fileInfoRepository
        )
        val me = supportTelegramBot.meAsync.get()
        val savedBot = botRepository.save(Bot(req.token, me.userName, me.firstName))
        supportTelegramBot.botId = savedBot.id!!
        supportTelegramBot.username = me.userName
        registerBot(supportTelegramBot)
        activeBots[req.token] = supportTelegramBot
        setDefaultBotCommands(supportTelegramBot)
    }

    fun registerBot(bot: SupportTelegramBot) {
        val telegramBot = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBot.registerBot(bot)
    }

    private fun stopBot(botId: Long) {
        findBotById(botId)?.let { tgBot ->
            val botOpt = botRepository.findById(tgBot.botId)
            if (botOpt.isPresent) {
                val bot = botOpt.get()
                bot.status = BotStatusEnum.STOPPED
                botRepository.save(bot)
                activeBots.remove(tgBot.token)
                return
            }
        }
        throw BotNotFoundException()
    }

    fun setDefaultBotCommands(bot: SupportTelegramBot) {
        val commandsEN = listOf(
            BotCommand("/start", bot.getMsgByLang("START", LanguageEnum.EN)),
            BotCommand("/setlang", bot.getMsgByLang("SET_LANG", LanguageEnum.EN))
        )
        val commandsRU = listOf(
            BotCommand("/start", bot.getMsgByLang("START", LanguageEnum.RU)),
            BotCommand("/setlang", bot.getMsgByLang("SET_LANG", LanguageEnum.RU))
        )
        val commandsUZ = listOf(
            BotCommand("/start", bot.getMsgByLang("START", LanguageEnum.UZ)),
            BotCommand("/setlang", bot.getMsgByLang("SET_LANG", LanguageEnum.UZ))
        )
        bot.execute(SetMyCommands(commandsEN, BotCommandScopeDefault(), "en"))
        bot.execute(SetMyCommands(commandsRU, BotCommandScopeDefault(), "ru"))
        bot.execute(SetMyCommands(commandsUZ, BotCommandScopeDefault(), "uz"))
    }

    fun getAllBots(): List<BotResponse> {
        return botRepository.findAllNotDeleted().map {
            BotResponse.torResponse(it)
        }
    }

    fun getAllActiveBots(): List<BotResponse> {
        return botRepository.findAllBotsByStatusAndDeletedFalse(BotStatusEnum.ACTIVE).map {
            BotResponse.torResponse(it)
        }
    }

    fun getOneBot(botId: Long): BotResponse? {
        return botRepository.findByIdAndDeletedFalse(botId)?.let {
            BotResponse.torResponse(it)
        } ?: throw BotNotFoundException()
    }

    fun deleteBot(botId: Long) {
        stopBot(botId)
        botRepository.deleteById(botId)
    }

}