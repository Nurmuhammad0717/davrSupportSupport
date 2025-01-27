package uz.davrmobile.support.bot.bot

import org.springframework.context.MessageSource
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import uz.davrmobile.support.bot.backend.*
import uz.davrmobile.support.bot.bot.Utils.Companion.clearPhone
import uz.davrmobile.support.bot.bot.Utils.Companion.htmlBold
import uz.davrmobile.support.bot.bot.Utils.Companion.randomHashId
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

open class SupportTelegramBot(
    var username: String,
    val token: String,
    var botId: Long,

    private val userRepository: UserRepository,
    private val botMessageRepository: BotMessageRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository,
    private val diceRepository: DiceRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: MessageSource,
    private val fileInfoRepository: FileInfoRepository,
    private val botRepository: BotRepository,
    private val messageToOperatorService: MessageToOperatorService,
    private val executorService: Executor = Executors.newFixedThreadPool(20),
) : TelegramLongPollingBot(token) {
    companion object {
        val activeBots = mutableMapOf<String, SupportTelegramBot>()

        fun findBotById(botId: Long): SupportTelegramBot? {
            for (bot in activeBots) if (bot.value.botId == botId) return bot.value
            return null
        }
    }

    override fun getBotUsername() = username

    @Transactional
    override fun onUpdateReceived(update: Update) {
        executorService.execute {
            try {
                if (update.hasMessage()) handleMessage(update)
                if (update.hasEditedMessage()) handleEditedMessage(update)
                if (update.hasCallbackQuery()) handleCallbackQuery(update)
                if (update.hasMyChatMember()) handleMyChatMember(update)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Transactional
    open fun handleMyChatMember(update: Update) {
        val chatMember = update.myChatMember
        val user = getUser(chatMember.from)

        if (user.isUser()) {
            val newStatus = chatMember.newChatMember.status
            if (newStatus == "kicked") {
                user.deleted = true
            } else if (newStatus == "member") {
                user.deleted = false
            }
            userRepository.save(user)
        }
    }

    @Transactional
    open fun handleMessage(update: Update) {
        val message = update.message

        getUser(message.from).let { user ->
            if (user.isUser()) {
                handleUserMessage(update, user)
            }
        }
    }

    @Transactional
    open fun handleUserMessage(update: Update, user: BotUser) {
        val chatId = user.id
        val message = update.message

        when (user.state) {
            UserStateEnum.NEW_USER -> {
                sendActionTyping(user)
                handleStartCommand(user)
            }

            UserStateEnum.SEND_PHONE_NUMBER -> {
                sendActionTyping(user)
                if (message.hasContact()) {
                    val contact = message.contact
                    val phoneNumber = contact.phoneNumber.clearPhone()

                    if (contact.userId != chatId) {
                        handleInvalidPhoneNumber(user)
                    } else {
                        saveUserPhoneNumber(user, phoneNumber)
                        sendEnterYourFullName(user)
                    }
                } else sendSharePhoneMsg(user)
            }

            UserStateEnum.SEND_FULL_NAME -> {
                sendActionTyping(user)
                if (message.hasText()) {
                    val text = message.text
                    if (text.contains("^[A-Za-z]+( [A-Za-z]+)*$".toRegex())) {
                        updateUserFullName(user, text)
                        sendFullNameSavedMsg(user)
                    } else sendEnterYourFullNameValid(user)
                } else sendEnterYourFullName(user)
            }

            UserStateEnum.CHOOSE_LANG -> {
                sendActionTyping(user)
                sendChooseLangMsg(user)
            }

            UserStateEnum.TALKING -> {
                checkAndHandleUserSession(user, update)
            }

            UserStateEnum.ACTIVE_USER -> {
                sendActionTyping(user)
                if (message.hasText()) {
                    val text = message.text
                    when (getMsgKeyByValue(text, user)) {
                        "CONNECT_WITH_OPERATOR" -> {
                            sendAskYourQuestionMsg(user)
                        }

                        else -> {
                            if (!handleCommonCommands(text, user)) sendMainMenuMsg(user)
                        }
                    }
                } else sendMainMenuMsg(user)
            }

            UserStateEnum.ASK_YOUR_QUESTION -> {
                sendActionTyping(user)
                user.state = UserStateEnum.WAITING_OPERATOR
                userRepository.save(user)
                handleSessionMsgForUser(update, user)
            }

            UserStateEnum.WAITING_OPERATOR -> {
                checkAndHandleUserSession(user, update)
            }
        }
    }

    @Transactional
    open fun checkAndHandleUserSession(user: BotUser, update: Update) {
        sessionRepository.findLastSessionByUserId(user.id)?.let { session ->
            if ((session.isBusy() || session.isWaiting()) && session.botId != botId) {
                findBotById(session.botId)?.let { bot ->
                    this.execute(
                        SendMessage(
                            user.id.toString(),
                            getMsg("YOU_HAVE_ALREADY_OPENED_A_SESSION_IN_ANOTHER_BOT", user) + " @${bot.username}"
                        )
                    )
                }
            } else handleSessionMsgForUser(update, user)
        }
    }

    private fun sendMainMenuMsg(user: BotUser) {
        val newText = """
            ${getMsg("MENU", user)}:
            
            /setLang - ${getMsg("SET_LANG", user)}
        """.trimIndent()
        val sendMessage = SendMessage(user.id.toString(), newText)
        if (user.isUser()) {
            val row1 = KeyboardRow(1)
            row1.add(KeyboardButton(getMsg("CONNECT_WITH_OPERATOR", user)))
            val markup = ReplyKeyboardMarkup(listOf(row1))
            markup.resizeKeyboard = true
            sendMessage.replyMarkup = markup
        }
        this.execute(sendMessage)
    }

    private fun sendFullNameSavedMsg(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("FULL_NAME_SAVED", user))
        val row1 = KeyboardRow(1)
        row1.add(KeyboardButton(getMsg("CONNECT_WITH_OPERATOR", user)))
        val markup = ReplyKeyboardMarkup(listOf(row1))
        markup.resizeKeyboard = true
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
    }

    private fun sendActionTyping(user: BotUser) {
        this.execute(SendChatAction(user.id.toString(), "typing", null))
    }

    @Transactional
    open fun handleSessionMsgForUser(update: Update, user: BotUser) {
        getSession(user).let { session ->
            val savedMessage = newSessionMsg(update, session, user)

            if (session.hasOperator()) {

            } else {
                user.botId = botId
                userRepository.save(user)
            }
        }
    }

    private fun saveLocation(message: Message): Location? {
        return if (message.hasLocation()) {
            val loc = message.location
            locationRepository.save(Location(loc.latitude, loc.longitude))
        } else null
    }

    private fun saveContact(message: Message): Contact? {
        return if (message.hasContact()) {
            val contact = message.contact
            contactRepository.save(Contact(contact.firstName, contact.phoneNumber))
        } else null
    }

    private fun saveDice(message: Message): Dice? {
        return if (message.hasDice()) {
            val dice = message.dice
            diceRepository.save(Dice(dice.value, dice.emoji))
        } else null
    }

    private fun newSessionMsg(update: Update, session: Session, user: BotUser): BotMessage? {
        val message = update.message
        return determineMessageType(message, user)?.let { typeAndFileId ->
            val messageReplyId = if (message.isReply) message.replyToMessage.messageId else null
            val location = saveLocation(message)
            val contact = saveContact(message)
            val dice = saveDice(message)

            val fileInfo = typeAndFileId.second?.let { downloadAndSaveFile(it) }

            botMessageRepository.save(
                BotMessage(
                    user = user,
                    session = session,
                    messageId = message.messageId,
                    replyMessageId = messageReplyId,
                    botMessageType = typeAndFileId.first,
                    text = message.text,
                    caption = message.caption,
                    files = fileInfo?.let { listOf(it) },
                    location = location,
                    contact = contact,
                    dice = dice
                )
            )
        }
    }

    fun saveFile(fileId: String): SavedTgFileResponse {
        val fileFromTelegram = this.execute(GetFile(fileId))
        val inputStream = this.downloadFileAsStream(fileFromTelegram)
        var fileName = fileFromTelegram.filePath.substringAfterLast("/")
        val fileSize = fileFromTelegram.fileSize
        val fileExtension = fileName.substringAfterLast(".")
        fileName = fileName.substringBeforeLast(".") + randomHashId() + "." + fileExtension
        val filePath = "./files/${LocalDate.now()}/$fileName"
        Paths.get(filePath).parent?.let { directoryPath ->
            if (!Files.exists(directoryPath)) Files.createDirectories(directoryPath)
        }
        FileOutputStream(filePath).use { outputStream ->
            inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
        }
        return SavedTgFileResponse(fileName, fileExtension, filePath, fileSize)
    }

    private fun downloadAndSaveFile(fileIdAndFileName: String): FileInfo {
        val uploadFileName = fileIdAndFileName.substringAfter("$%%%$")
        val fileId = fileIdAndFileName.substringBefore("$%%%$")

        val file = saveFile(fileId)
        return fileInfoRepository.save(FileInfo(file.name, uploadFileName, file.extension, file.path, file.size))
    }

    private fun saveUserPhoneNumber(user: BotUser, phoneNumber: String) {
        user.phoneNumber = phoneNumber
        userRepository.save(user)
    }

    private fun handleInvalidPhoneNumber(user: BotUser) {
        sendWrongNumberMsg(user)
    }

    private fun handleStartCommand(user: BotUser) {
        if (user.languages.isEmpty()) {
            sendChooseLangMsg(user)
        } else {
            sendAskYourQuestionMsg(user)
        }
    }

    private fun updateUserFullName(user: BotUser, text: String) {
        user.fullName = text
        user.state = UserStateEnum.ACTIVE_USER
        userRepository.save(user)
    }

    @Transactional
    open fun handleEditedMessage(update: Update) {
        val editedMessage = update.editedMessage
        val chatId = editedMessage.from.id
        val messageId = editedMessage.messageId
        val editedText = editedMessage.text
        val editedCaption = editedMessage.caption

        editMessage(chatId, messageId, editedText, editedCaption)
    }

    @Transactional
    open fun handleCallbackQuery(update: Update) {
        val callbackQuery = update.callbackQuery
        var data = callbackQuery.data

        getUser(callbackQuery.from).let { user ->
            val chatId = user.id

            if (user.isUser()) {
                when (user.state) {
                    UserStateEnum.NEW_USER -> {}
                    UserStateEnum.SEND_PHONE_NUMBER -> {}
                    UserStateEnum.SEND_FULL_NAME -> {}
                    UserStateEnum.ACTIVE_USER -> {}
                    UserStateEnum.ASK_YOUR_QUESTION -> {}
                    UserStateEnum.TALKING -> {}
                    UserStateEnum.WAITING_OPERATOR -> {}
                    UserStateEnum.CHOOSE_LANG -> {
                        if (data.startsWith("setLang", ignoreCase = true)) {
                            val lang = LanguageEnum.valueOf(data.uppercase().substring("setlang".length))

                            user.languages = mutableSetOf(lang)
                            user.state = UserStateEnum.ACTIVE_USER
                            userRepository.save(user)
                            this.execute(DeleteMessage(chatId.toString(), callbackQuery.message.messageId))

                            if (user.phoneNumber.isEmpty()) {
                                sendSharePhoneMsg(user)
                            } else sendMainMenuMsg(user)
                        }
                    }
                }
                if (data.startsWith("rateS")) {
                    data = data.substring("rateS".length)
                    val rate = data.substring(0, 1).toShort()
                    val sessionId = data.substring(1).toLong()

                    setRate(sessionId, rate)
                    this.execute(
                        AnswerCallbackQuery(
                            callbackQuery.id, getMsg("THANK_YOU", user), true, null, null
                        )
                    )
                    this.execute(DeleteMessage(chatId.toString(), callbackQuery.message.messageId))
                }
            }
        }
    }

    @Transactional
    open fun editMessage(chatId: Long, messageId: Int, editedText: String?, editedCaption: String?) {
        botMessageRepository.findByUserIdAndMessageId(chatId, messageId)?.let { message ->
            messageToOperatorService.editMessage(editedText, editedCaption, message)
        }
    }

    @Synchronized
    fun getUser(from: org.telegram.telegrambots.meta.api.objects.User): BotUser {
        val userOpt = userRepository.findById(from.id)
        if (userOpt.isPresent) {
            return userOpt.get()
        }
        var username = from.userName
        if (username == null) username = ""
        var lastName = from.lastName
        lastName = if (lastName == null) "" else " $lastName"
        return userRepository.save(
            BotUser(
                from.id, username, from.firstName + lastName, "", botId
            )
        )
    }

    @Transactional
    open fun getSession(user: BotUser): Session {
        val session = sessionRepository.findLastSessionByUserId(user.id)
        return if (session != null) {
            if (session.isClosed()) {
                this.execute(
                    SendMessage(
                        user.id.toString(), getMsg("THE_OPERATOR_WILL_ANSWER_YOU_SOON", user)
                    )
                )
                user.let { sessionRepository.save(Session(it, botId, language = user.languages.elementAt(0))) }
            } else {
                session
            }
        } else {
            this.execute(SendMessage(user.id.toString(), getMsg("THE_OPERATOR_WILL_ANSWER_YOU_SOON", user)))
            user.let { sessionRepository.save(Session(it, botId, language = user.languages.elementAt(0))) }
        }
    }

    @Transactional
    open fun setRate(sessionId: Long, rate: Short): Session? {
        return sessionRepository.findByIdAndDeletedFalse(sessionId)?.let {
            if (it.status == SessionStatusEnum.CLOSED) {
                it.rate = rate
                sessionRepository.save(it)
            }
            it
        }
    }

    open fun determineMessageType(message: Message, user: BotUser): Pair<BotMessageType, String?>? {
        return when {
            message.hasText() -> Pair(BotMessageType.TEXT, null)
            message.hasPhoto() -> {
                Pair(BotMessageType.PHOTO, message.photo.maxByOrNull { it.fileSize ?: 0 }?.fileId)
            }

            message.hasVideoNote() -> Pair(BotMessageType.VIDEO_NOTE, message.videoNote.fileId)
            message.hasPoll() -> Pair(BotMessageType.POLL, null)
            message.hasVoice() -> Pair(BotMessageType.VOICE, message.voice.fileId)
            message.hasVideo() -> Pair(BotMessageType.VIDEO, message.video.fileId)
            message.hasAudio() -> Pair(BotMessageType.AUDIO, message.audio.fileId)
            message.hasContact() -> Pair(BotMessageType.CONTACT, null)
            message.hasLocation() -> Pair(BotMessageType.LOCATION, null)
            message.hasDice() -> Pair(BotMessageType.DICE, null)
            message.hasSticker() -> Pair(BotMessageType.STICKER, message.sticker.fileId)
            message.hasAnimation() -> Pair(BotMessageType.ANIMATION, message.animation.fileId)
            message.hasDocument() -> {
                if (message.document.fileSize >= 2e+7) {
                    sendMaxFileSize20MBWarningMsg(user)
                    null
                } else Pair(BotMessageType.DOCUMENT, message.document.fileId + "$%%%$" + message.document.fileName)
            }

            else -> {
                null
            }
        }
    }

    private fun sendMaxFileSize20MBWarningMsg(user: BotUser) {
        this.execute(SendMessage(user.id.toString(), getMsg("MAX_FILE_SIZE_20_MB", user)))
    }

    @Transactional
    open fun handleCommonCommands(text: String, user: BotUser): Boolean {
        return when (text.lowercase()) {
            "/setlang" -> {
                sendChooseLangMsg(user)
                true
            }

            "/setname" -> {
                sendEnterYourFullName(user)
                true
            }

            else -> false
        }
    }

    @Transactional
    open fun stopChat(operator: BotUser) {
        val session = sessionRepository.findByOperatorIdAndStatus(operator.id, SessionStatusEnum.BUSY)
        session?.let {
            val user = it.user

            it.status = SessionStatusEnum.CLOSED
            it.operatorId = null
            userRepository.save(operator)
            user.state = UserStateEnum.ACTIVE_USER
            sessionRepository.save(it)
            userRepository.save(user)

            sendChatStoppedMsg(operator)
            sendRateMsg(user, session)
            sendMainMenuMsg(user)
        }
    }

    open fun sendAskYourQuestionMsg(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("ASK_YOUR_QUESTION", user).htmlBold())
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        sendMessage.parseMode = ParseMode.HTML
        user.state = UserStateEnum.ASK_YOUR_QUESTION
        userRepository.save(user)
        this.execute(sendMessage)
    }

    open fun sendWrongNumberMsg(user: BotUser) {
        this.execute(SendMessage(user.id.toString(), getMsg("WRONG_NUMBER", user)))
    }

    open fun sendRateMsg(user: BotUser, session: Session) {
        val sendMessage = SendMessage(
            user.id.toString(), getMsg("OPERATOR_STOPPED_CHAT", user) + "\n" + getMsg("PLEASE_RATE_OPERATOR_WORK", user)
        )
        val btn1 = InlineKeyboardButton(getMsg("VERY_BAD", user))
        btn1.callbackData = "rateS1" + session.id
        val btn2 = InlineKeyboardButton(getMsg("BAD", user))
        btn2.callbackData = "rateS2" + session.id
        val btn3 = InlineKeyboardButton(getMsg("SATISFACTORY", user))
        btn3.callbackData = "rateS3" + session.id
        val btn4 = InlineKeyboardButton(getMsg("GOOD", user))
        btn4.callbackData = "rateS4" + session.id
        val btn5 = InlineKeyboardButton(getMsg("EXCELLENT", user))
        btn5.callbackData = "rateS5" + session.id
        val markup = InlineKeyboardMarkup(listOf(listOf(btn1), listOf(btn2), listOf(btn3), listOf(btn4), listOf(btn5)))
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
    }

    open fun sendChatStoppedMsg(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("CHAT_STOPPED", user).htmlBold())
        sendMessage.parseMode = ParseMode.HTML
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        this.execute(sendMessage)
    }

    open fun getMsg(key: String, user: BotUser): String {
        try {
            val locale = Locale.forLanguageTag(user.languages.elementAt(0).name.lowercase())
            return messageSource.getMessage(key, null, locale)
        } catch (e: Exception) {
            return "Error"
        }
    }

    open fun getMsgByLang(key: String, languageEnum: LanguageEnum): String {
        try {
            val locale = Locale.forLanguageTag(languageEnum.name.lowercase())
            return messageSource.getMessage(key, null, locale)
        } catch (e: Exception) {
            return "Error"
        }
    }

    open fun getMsgKeyByValue(value: String, user: BotUser): String {
        for (language in user.languages) {
            val locale = Locale.forLanguageTag(language.name.lowercase())
            val bundle = ResourceBundle.getBundle("bot_messages", locale)
            for (key in bundle.keySet()) if (bundle.getString(key) == value) return key
        }
        return ""
    }

    open fun sendChooseLangMsg(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), "Choose language")
        val btn1 = InlineKeyboardButton("🇺🇸 English")
        btn1.callbackData = "setLangEN"
        val btn2 = InlineKeyboardButton("🇷🇺 Русский")
        btn2.callbackData = "setLangRU"
        val btn3 = InlineKeyboardButton("🇺🇿 O'zbek")
        btn3.callbackData = "setLangUZ"
        val markup = InlineKeyboardMarkup(listOf(listOf(btn1), listOf(btn2), listOf(btn3)))
        sendMessage.replyMarkup = markup
        val msgId = this.execute(sendMessage).messageId
        user.msgIdChooseLanguage = msgId
        user.state = UserStateEnum.CHOOSE_LANG
        userRepository.save(user)
    }

    open fun sendSharePhoneMsg(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("CLICK_TO_SEND_YOUR_PHONE", user))
        val keyboardButton = KeyboardButton(getMsg("SHARE_PHONE_NUMBER", user))
        keyboardButton.requestContact = true
        val row = KeyboardRow(1)
        row.add(keyboardButton)
        val markup = ReplyKeyboardMarkup(listOf(row))
        markup.resizeKeyboard = true
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
        user.state = UserStateEnum.SEND_PHONE_NUMBER
        userRepository.save(user)
    }

    open fun sendEnterYourFullName(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("SEND_YOUR_FULL_NAME", user))
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        this.execute(sendMessage)
        user.state = UserStateEnum.SEND_FULL_NAME
        userRepository.save(user)
    }

    open fun sendEnterYourFullNameValid(user: BotUser) {
        val sendMessage = SendMessage(user.id.toString(), getMsg("SEND_YOUR_FULL_NAME_ONLY_LETTERS", user))
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        this.execute(sendMessage)
        user.state = UserStateEnum.SEND_FULL_NAME
        userRepository.save(user)
    }

    open fun getStatusEmojiByBoolean(t: Boolean): String {
        return if (t) "✅" else "❌"
    }
}