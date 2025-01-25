package uz.davrmobile.support.bot.bot

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import uz.davrmobile.support.bot.backend.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.activeBots
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.findBotById
import uz.davrmobile.support.util.getUserId


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
    private val messageToOperatorServiceImpl: MessageToOperatorServiceImpl,
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
            fileInfoRepository,
            botRepository,
            messageToOperatorServiceImpl
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

    fun stopBot(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            findBotById(bot.id!!)?.let { tgBot ->
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
        return updateAllBotsAndGet(botRepository.findAllByDeletedFalse()).map {
            val res = BotResponse.torResponse(it)
            res.token = null
            res
        }
    }

    fun getAllActiveBots(): List<BotResponse> {
        return updateAllBotsAndGet(botRepository.findAllBotsByStatusAndDeletedFalse(BotStatusEnum.ACTIVE)).map {
            val res = BotResponse.torResponse(it)
            res.token = null
            res
        }
    }

    private fun updateBotInfo(bot: Bot): Bot {
        findBotById(bot.id!!)?.let {
            val user = it.execute(GetMe())
            bot.username = user.userName
            bot.name = user.firstName
        }
        return bot
    }

    private fun updateAllBotsAndGet(bots: List<Bot>): List<Bot> {
        return bots.map {
            botRepository.save(updateBotInfo(it))
        }
    }

    fun getOneBot(id: String): BotResponse? {
        return botRepository.findByHashIdAndDeletedFalse(id)?.let {
            BotResponse.torResponse(it)
        } ?: throw BotNotFoundException()
    }

    fun deleteBot(id: String) {
        stopBot(id)
        botRepository.deleteByHashId(id)
    }

    fun addBotToOperator(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            bot.operatorIds.add(getUserId())
            botRepository.save(bot)
        } ?: run {
            throw BotNotFoundException()
        }
    }

    fun removeBotFromOperator(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            val operatorId = getUserId()
            if (bot.operatorIds.contains(operatorId)) {
                bot.operatorIds.remove(operatorId)
                botRepository.save(bot)
            }
        } ?: run {
            throw BotNotFoundException()
        }
    }
}