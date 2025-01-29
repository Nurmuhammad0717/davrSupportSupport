package uz.davrmobile.support.bot.bot

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import uz.davrmobile.support.bot.backend.*
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.activeBots
import uz.davrmobile.support.bot.bot.SupportTelegramBot.Companion.findBotById
import uz.davrmobile.support.util.userId

interface BotService {
    fun createBot(req: TokenRequest)
    fun registerBot(bot: SupportTelegramBot)
    fun changeStatus(id: String, status: String)
    fun getAllBots(): List<BotResponse>
    fun getAllActiveBots(): List<BotResponse>
    fun getOneBot(id: String): BotResponse?
    fun deleteBot(id: String)
    fun addBotToOperator(id: String)
    fun removeBotFromOperator(id: String)
}

@Service
class BotServiceImpl(
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
) : BotService {

    override fun createBot(req: TokenRequest) {
        if (!botRepository.existsByToken((req.token))) {
            val supportTelegramBot = createSupportTelegramBot(req.token)
            try {
                val me = supportTelegramBot.meAsync.get()
                val savedBot = botRepository.save(Bot(me.id, req.token, me.userName, me.firstName))
                supportTelegramBot.botId = savedBot.chatId
                supportTelegramBot.username = me.userName
                startBot(supportTelegramBot)
                setDefaultBotCommands(supportTelegramBot)
            } catch (e: TelegramApiRequestException) {
                if (e.errorCode == 401) throw BotTokenNotValidException()
                else throw BadCredentialsException()
            }
        }
    }

    private fun createSupportTelegramBot(token: String): SupportTelegramBot {
        return SupportTelegramBot(
            "",
            token,
            -1L,
            userRepository,
            botMessageRepository,
            locationRepository,
            contactRepository,
            diceRepository,
            sessionRepository,
            messageSource,
            fileInfoRepository,
            messageToOperatorServiceImpl
        )
    }

    private fun startBot(bot: SupportTelegramBot) {
        registerBot(bot)
        activeBots[bot.token] = bot
    }

    override fun registerBot(bot: SupportTelegramBot) {
        val telegramBot = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBot.registerBot(bot)
    }

    override fun changeStatus(id: String, status: String) {
        if (status == "stop") {
            botRepository.findByHashId(id)?.let { bot ->
                findBotById(bot.chatId)?.let { tgBot ->
                    bot.status = BotStatusEnum.STOPPED
                    botRepository.save(bot)
                    activeBots.remove(tgBot.token)
                } ?: throw BotAlreadyStoppedException()
            } ?: throw BotNotFoundException()
        } else if (status == "active") {
            botRepository.findByHashId(id)?.let { bot ->
                findBotById(bot.chatId)?.let {
                    throw BotAlreadyActiveException()
                } ?: run {
                    findBotById(bot.chatId) ?: run {
                        val tgBot = createSupportTelegramBot(bot.token)
                        val me = tgBot.meAsync.get()
                        tgBot.botId = bot.chatId
                        tgBot.username = me.userName
                        updateBotPhoto(tgBot, bot)
                        startBot(tgBot)
                    }
                }
            } ?: throw BotNotFoundException()
        }
    }

    private fun updateBotPhoto(tgBot: SupportTelegramBot, bot: Bot) {
        val photos = getBotPhotos(tgBot).photos
        if (photos.size > 0) {
            val photo = photos[0]
            val miniPhotoSize = photo[0]
            val bigPhotoSize = photo[photo.lastIndex]
            bot.miniPhotoId?.let {
                fileInfoRepository.findByHashId(it)?.let { fileInfo ->
                    if (miniPhotoSize.fileSize.toLong() == fileInfo.size) return
                }
            }
            bot.miniPhotoId = fileInfoRepository.save(tgBot.saveFile(miniPhotoSize.fileId).toEntity()).hashId
            bot.bigPhotoId =
                if (miniPhotoSize.fileId == bigPhotoSize.fileId)
                    fileInfoRepository.save(tgBot.saveFile(bigPhotoSize.fileId).toEntity()).hashId
                else bot.miniPhotoId
            botRepository.save(bot)
        }
    }

    private fun getBotPhotos(tgBot: SupportTelegramBot): UserProfilePhotos {
        return tgBot.execute(GetUserProfilePhotos(tgBot.botId))
    }

    private fun setDefaultBotCommands(bot: SupportTelegramBot) {
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

    override fun getAllBots(): List<BotResponse> {
        return updateAllBotsAndGet(botRepository.findAllByDeletedFalse())
            .map { BotResponse.toResponse(it) }
    }

    override fun getAllActiveBots(): List<BotResponse> {
        return updateAllBotsAndGet(botRepository.findAllBotsByStatusAndDeletedFalse(BotStatusEnum.ACTIVE))
            .map { BotResponse.toResponseWithoutToken(it) }
    }

    private fun updateBotInfo(bot: Bot): Bot {
        findBotById(bot.chatId)?.let {
            val user = it.execute(GetMe())
            updateBotPhoto(it, bot)
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

    override fun getOneBot(id: String): BotResponse? {
        return botRepository.findByHashIdAndDeletedFalse(id)?.let {
            BotResponse.toResponse(it)
        } ?: throw BotNotFoundException()
    }

    @Transactional
    override fun deleteBot(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            findBotById(bot.chatId)?.let { tgBot ->
                bot.status = BotStatusEnum.STOPPED
                botRepository.save(bot)
                activeBots.remove(tgBot.token)
            }
        } ?: throw BotNotFoundException()
        botRepository.deleteByHashId(id)
    }

    override fun addBotToOperator(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            bot.operatorIds.add(userId())
            botRepository.save(bot)
        } ?: run {
            throw BotNotFoundException()
        }
    }

    override fun removeBotFromOperator(id: String) {
        botRepository.findByHashId(id)?.let { bot ->
            val operatorId = userId()
            if (bot.operatorIds.contains(operatorId)) {
                bot.operatorIds.remove(operatorId)
                botRepository.save(bot)
            }
        } ?: run {
            throw BotNotFoundException()
        }
    }
}