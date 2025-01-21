package uz.davrmobile.support.bot.bot

import org.springframework.context.MessageSource
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors

open class SupportTelegramBot(
    var username: String,
    token: String,
    var botId: Long,

    private val userRepository: UserRepository,
    private val botMessageRepository: BotMessageRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: ContactRepository,
    private val diceRepository: DiceRepository,
    private val sessionRepository: SessionRepository,
    private val messageSource: MessageSource,
    private val executorService: Executor = Executors.newFixedThreadPool(20),
    val queueEn: ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>> = ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>>(),
    val queueUz: ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>> = ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>>(),
    val queueRu: ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>> = ConcurrentHashMap<Long, CopyOnWriteArrayList<BotMessage>>()
) : TelegramLongPollingBot(token) {
    companion object {
        val activeBots = mutableMapOf<String, SupportTelegramBot>()
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
            sendActionTyping(user)

            if (user.isUser()) {
                handleUserMessage(update, user)
            }
//            else if (user.isOperator()) {
//                handleOperatorMessage(update, user)
//            }
        }
    }
    @Transactional
    open fun handleUserMessage(update: Update, botUser: BotUser) {
        val chatId = botUser.id
        val message = update.message

        when (botUser.state) {
            UserStateEnum.NEW_USER -> {
                handleStartCommand(botUser)
            }

            UserStateEnum.SEND_PHONE_NUMBER -> {
                if (message.hasContact()) {
                    val contact = message.contact
                    val phoneNumber = contact.phoneNumber.clearPhone()

                    if (contact.userId != chatId) {
                        handleInvalidPhoneNumber(botUser)
                    } else {
                        saveUserPhoneNumber(botUser, phoneNumber)
                        sendEnterYourFullName(botUser)
                    }
                } else {
                    sendSharePhoneMsg(botUser)
                }
            }

            UserStateEnum.SEND_FULL_NAME -> {
                if (message.hasText()) {
                    val text = message.text
                    updateUserFullName(botUser, text)
                    sendFullNameSavedMsg(botUser)
                } else {
                    sendEnterYourFullName(botUser)
                }
            }

            UserStateEnum.CHOOSE_LANG -> {
                sendChooseLangMsg(botUser)
            }

            UserStateEnum.TALKING -> {
                checkAndHandleUserSession(botUser, update)
            }

            UserStateEnum.ACTIVE_USER -> {
                if (message.hasText()) {
                    val text = message.text
                    when (getMsgKeyByValue(text, botUser)) {
                        "CONNECT_WITH_OPERATOR" -> {
                            sendAskYourQuestionMsg(botUser)
                        }

                        else -> {
                            if (!handleCommonCommands(text, botUser)) sendMainMenuMsg(botUser)
                        }
                    }
                } else sendMainMenuMsg(botUser)
            }

            UserStateEnum.ASK_YOUR_QUESTION -> {
                botUser.state = UserStateEnum.WAITING_OPERATOR
                userRepository.save(botUser)
                handleSessionMsgForUser(update, botUser)
            }

            UserStateEnum.WAITING_OPERATOR -> {
                checkAndHandleUserSession(botUser, update)
            }
        }
    }

    @Transactional
    open fun checkAndHandleUserSession(botUser: BotUser, update: Update) {
        sessionRepository.findLastSessionByUserId(botUser.id)?.let { session ->
            if (session.isBusy() && session.botId != botId) {
                findBotById(session.botId)?.let { bot ->
                    this.execute(
                        SendMessage(
                            botUser.id.toString(),
                            getMsg("YOU_HAVE_ALREADY_OPENED_A_SESSION_IN_ANOTHER_BOT", botUser) + " @${bot.username}"
                        )
                    )
                }
            } else handleSessionMsgForUser(update, botUser)
        }
    }

    private fun findBotById(botId: Long): SupportTelegramBot? {
        for (bot in activeBots) if (bot.value.botId == botId) return bot.value
        return null
    }

    private fun sendMainMenuMsg(botUser: BotUser) {
        val newText = """
            ${getMsg("MENU", botUser)}:
            
            /setLang - ${getMsg("SET_LANG", botUser)}
        """.trimIndent()
        val sendMessage = SendMessage(botUser.id.toString(), newText)
        if (botUser.isUser()) {
            val row1 = KeyboardRow(1)
            row1.add(KeyboardButton(getMsg("CONNECT_WITH_OPERATOR", botUser)))
            val markup = ReplyKeyboardMarkup(listOf(row1))
            markup.resizeKeyboard = true
            sendMessage.replyMarkup = markup
        }
        this.execute(sendMessage)
    }

    private fun sendFullNameSavedMsg(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), getMsg("FULL_NAME_SAVED", botUser))
        val row1 = KeyboardRow(1)
        row1.add(KeyboardButton(getMsg("CONNECT_WITH_OPERATOR", botUser)))
        val markup = ReplyKeyboardMarkup(listOf(row1))
        markup.resizeKeyboard = true
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
    }

    private fun sendActionTyping(botUser: BotUser) {
        this.execute(SendChatAction(botUser.id.toString(), "typing", null))
    }

    @Transactional
    open fun handleSessionMsgForUser(update: Update, botUser: BotUser) {
        getSession(botUser).let { session ->
            val savedMessage = newSessionMsg(update, session, botUser)

            if (session.hasOperator()) {

            } else {
                botUser.botId = botId
                userRepository.save(botUser)


            }
        }
    }

    private fun saveLocation(message: Message): Location? {
        return if (message.hasLocation()) {
            val loc = message.location
            locationRepository.save(Location(loc.latitude.toFloat(), loc.longitude.toFloat()))
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

    private fun newSessionMsg(update: Update, session: Session, botUser: BotUser): BotMessage {
        val message = update.message
        val messageReplyId = if (message.isReply) message.replyToMessage.messageId else null
        val typeAndFileId = determineMessageType(message)
        val location = saveLocation(message)
        val contact = saveContact(message)
        val dice = saveDice(message)

        return botMessageRepository.save(
            BotMessage(
                botUser = botUser,
                session = session,
                messageId = message.messageId,
                replyMessageId = messageReplyId,
                botMessageType = typeAndFileId.first,
                text = message.text,
                caption = message.caption,
                fileId = typeAndFileId.second,
                location = location,
                contact = contact,
                dice = dice
            )
        )
    }

    private fun saveUserPhoneNumber(botUser: BotUser, phoneNumber: String) {
        botUser.phoneNumber = phoneNumber
        userRepository.save(botUser)
    }

    private fun handleInvalidPhoneNumber(botUser: BotUser) {
        sendWrongNumberMsg(botUser)
    }

    private fun handleStartCommand(botUser: BotUser) {
        if (botUser.languages.isEmpty()) {
            sendChooseLangMsg(botUser)
        } else {
            sendAskYourQuestionMsg(botUser)
        }
    }

    private fun updateUserFullName(botUser: BotUser, text: String) {
        botUser.fullName = text
        botUser.state = UserStateEnum.ACTIVE_USER
        userRepository.save(botUser)
    }

    @Transactional
    open fun handleEditedMessage(update: Update) {
        val editedMessage = update.editedMessage
        val chatId = editedMessage.from.id
        val messageId = editedMessage.messageId
        val newText = editedMessage.text
        val newCaption = editedMessage.caption

        editMessage(chatId, messageId, newText, newCaption)
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
                            callbackQuery.id, getMsg("THANK_YOU", user), false, null, null
                        )
                    )
                    this.execute(DeleteMessage(chatId.toString(), callbackQuery.message.messageId))
                }
            }
        }
    }

    @Transactional
    open fun editMessage(chatId: Long, messageId: Int, newText: String?, newCaption: String?) {
        val message = botMessageRepository.findByUserIdAndMessageId(chatId, messageId)
            ?: throw IllegalArgumentException("Message with ID $messageId not found")

        if (!newText.isNullOrBlank() && message.botMessageType == BotMessageType.TEXT) {
            message.text = newText
            if (message.botMessageId != null) {
                if (message.session.botUser.id == chatId) {
                    val editMessage = EditMessageText(newText)
                    editMessage.messageId = message.botMessageId!!
                    editMessage.chatId = message.session.operatorId.toString()
                    this.execute(editMessage)
                } else {
                    val editMessage = EditMessageText(newText)
                    editMessage.messageId = message.botMessageId!!
                    editMessage.chatId = message.session.botUser.id.toString()
                    this.execute(editMessage)
                }
            }
        }

        if (!newCaption.isNullOrBlank() &&
            message.botMessageType in listOf(
                BotMessageType.PHOTO,
                BotMessageType.VIDEO,
                BotMessageType.DOCUMENT,
                BotMessageType.ANIMATION
            )
        ) {
            message.caption = newCaption
            if (message.botMessageId != null) {
                val editMessage = EditMessageCaption()
                editMessage.caption = newCaption
                editMessage.messageId = message.botMessageId!!
                if (message.session.botUser.id == chatId) {
                    editMessage.chatId = message.session.operatorId.toString()
                } else {
                    editMessage.chatId = message.session.botUser.id.toString()
                }
                this.execute(editMessage)
            }
        }
        botMessageRepository.save(message)
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
    open fun getSession(botUser: BotUser): Session {
        val session = sessionRepository.findLastSessionByUserId(botUser.id)
        return if (session != null) {
            if (session.isClosed()) {
                this.execute(
                    SendMessage(
                        botUser.id.toString(), getMsg("THE_OPERATOR_WILL_ANSWER_YOU_SOON", botUser)
                    )
                )
                botUser.let { sessionRepository.save(Session(it, botId)) }
            } else {
                session
            }
        } else {
            this.execute(SendMessage(botUser.id.toString(), getMsg("THE_OPERATOR_WILL_ANSWER_YOU_SOON", botUser)))
            botUser.let { sessionRepository.save(Session(it, botId)) }
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

    open fun determineMessageType(message: Message): Pair<BotMessageType, String?> {
        return when {
            message.hasText() -> Pair(BotMessageType.TEXT, null)
            message.hasPhoto() -> {
                Pair(BotMessageType.PHOTO, message.photo.maxByOrNull { it.fileSize ?: 0 }?.fileId)
            }

            message.hasVideoNote() -> Pair(BotMessageType.VIDEO, message.videoNote.fileId)
            message.hasPoll() -> Pair(BotMessageType.POLL, null)
            message.hasVoice() -> Pair(BotMessageType.VOICE, message.voice.fileId)
            message.hasVideo() -> Pair(BotMessageType.VIDEO_NOTE, message.video.fileId)
            message.hasAudio() -> Pair(BotMessageType.AUDIO, message.audio.fileId)
            message.hasContact() -> Pair(BotMessageType.CONTACT, null)
            message.hasLocation() -> Pair(BotMessageType.LOCATION, null)
            message.hasDice() -> Pair(BotMessageType.DICE, null)
            message.hasSticker() -> Pair(BotMessageType.STICKER, message.sticker.fileId)
            message.hasAnimation() -> Pair(BotMessageType.ANIMATION, message.animation.fileId)
            message.hasDocument() -> Pair(BotMessageType.DOCUMENT, message.document.fileId)
            else -> throw RuntimeException("un support type: $message")
        }
    }

    @Transactional
    open fun handleCommonCommands(text: String, botUser: BotUser): Boolean {
        return when (text.lowercase()) {
            "/setlang" -> {
                sendChooseLangMsg(botUser)
                true
            }

            "/setname" -> {
                sendEnterYourFullName(botUser)
                true
            }

            else -> false
        }
    }

    @Transactional
    open fun stopChat(operator: BotUser) {
        val session = sessionRepository.findByOperatorIdAndStatus(operator.id, SessionStatusEnum.BUSY)
        session?.let {
            val user = it.botUser

            it.status = SessionStatusEnum.CLOSED
            it.operatorId = null
            userRepository.save(operator)
            user.state = UserStateEnum.ACTIVE_USER
            sessionRepository.save(it)
            userRepository.save(user)

            sendChatStoppedMsg(operator)
            sendRateMsg(user, operator, session)
            sendMainMenuMsg(user)
        }
    }

    open fun sendAskYourQuestionMsg(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), getMsg("ASK_YOUR_QUESTION", botUser).htmlBold())
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        sendMessage.parseMode = ParseMode.HTML
        botUser.state = UserStateEnum.ASK_YOUR_QUESTION
        userRepository.save(botUser)
        this.execute(sendMessage)
    }

    open fun sendWrongNumberMsg(botUser: BotUser) {
        this.execute(SendMessage(botUser.id.toString(), getMsg("WRONG_NUMBER", botUser)))
    }

    open fun sendRateMsg(botUser: BotUser, operator: BotUser, session: Session) {
        val sendMessage = SendMessage(
            botUser.id.toString(),
            getMsg("OPERATOR_STOPPED_CHAT", operator) + "\n" + getMsg("PLEASE_RATE_OPERATOR_WORK", operator)
        )
        val btn1 = InlineKeyboardButton(getMsg("VERY_BAD", botUser))
        btn1.callbackData = "rateS1" + session.id
        val btn2 = InlineKeyboardButton(getMsg("BAD", botUser))
        btn2.callbackData = "rateS2" + session.id
        val btn3 = InlineKeyboardButton(getMsg("SATISFACTORY", botUser))
        btn3.callbackData = "rateS3" + session.id
        val btn4 = InlineKeyboardButton(getMsg("GOOD", botUser))
        btn4.callbackData = "rateS4" + session.id
        val btn5 = InlineKeyboardButton(getMsg("EXCELLENT", botUser))
        btn5.callbackData = "rateS5" + session.id
        val markup = InlineKeyboardMarkup(listOf(listOf(btn1), listOf(btn2), listOf(btn3), listOf(btn4), listOf(btn5)))
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
    }

    open fun sendChatStoppedMsg(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), getMsg("CHAT_STOPPED", botUser).htmlBold())
        sendMessage.parseMode = ParseMode.HTML
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        this.execute(sendMessage)
    }

    open fun getMsg(key: String, botUser: BotUser): String {
        try {
            val locale = Locale.forLanguageTag(botUser.languages.elementAt(0).name.lowercase())
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

    open fun getMsgKeyByValue(value: String, botUser: BotUser): String {
        for (language in botUser.languages) {
            val locale = Locale.forLanguageTag(language.name.lowercase())
            val bundle = ResourceBundle.getBundle("messages", locale)
            for (key in bundle.keySet()) if (bundle.getString(key) == value) return key
        }
        return ""
    }

    open fun sendChooseLangMsg(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), "Choose language")
        val btn1 = InlineKeyboardButton("🇺🇸 O'zbek")
        btn1.callbackData = "setLangEN"
        val btn2 = InlineKeyboardButton("🇷🇺 Русский")
        btn2.callbackData = "setLangRU"
        val btn3 = InlineKeyboardButton("🇺🇿 English")
        btn3.callbackData = "setLangUZ"
        val markup = InlineKeyboardMarkup(listOf(listOf(btn1), listOf(btn2), listOf(btn3)))
        sendMessage.replyMarkup = markup
        val msgId = this.execute(sendMessage).messageId
        botUser.msgIdChooseLanguage = msgId
        botUser.state = UserStateEnum.CHOOSE_LANG
        userRepository.save(botUser)
    }

    open fun sendSharePhoneMsg(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), getMsg("CLICK_TO_SEND_YOUR_PHONE", botUser))
        val keyboardButton = KeyboardButton(getMsg("SHARE_PHONE_NUMBER", botUser))
        keyboardButton.requestContact = true
        val row = KeyboardRow(1)
        row.add(keyboardButton)
        val markup = ReplyKeyboardMarkup(listOf(row))
        markup.resizeKeyboard = true
        sendMessage.replyMarkup = markup
        this.execute(sendMessage)
        botUser.state = UserStateEnum.SEND_PHONE_NUMBER
        userRepository.save(botUser)
    }

    open fun sendEnterYourFullName(botUser: BotUser) {
        val sendMessage = SendMessage(botUser.id.toString(), getMsg("SEND_YOUR_FULL_NAME", botUser))
        sendMessage.replyMarkup = ReplyKeyboardRemove(true)
        this.execute(sendMessage)
        botUser.state = UserStateEnum.SEND_FULL_NAME
        userRepository.save(botUser)
    }

    open fun getStatusEmojiByBoolean(t: Boolean): String {
        return if (t) "✅" else "❌"
    }
}