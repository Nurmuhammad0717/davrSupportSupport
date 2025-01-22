package uz.davrmobile.support.bot.backend

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile
import uz.davrmobile.support.bot.bot.BotService
import uz.davrmobile.support.usecase.SendMessageUseCase
import uz.davrmobile.support.util.IsModerator
import uz.davrmobile.support.util.IsUser


@RestController
@RequestMapping("/bot")
class BotController(private val botService: BotService) {
    @IsModerator
    @PostMapping
    fun create(@RequestBody req: TokenRequest) = botService.createBot(req)

    @IsModerator
    @GetMapping
    fun getAll() = botService.getAllBots()

    @IsModerator
    @GetMapping("{id}")
    fun getOneBot(@PathVariable id: String) = botService.getOneBot(id)

    @IsModerator
    @DeleteMapping("{id}")
    fun deleteBot(@PathVariable id: String) = botService.deleteBot(id)

    @IsModerator
    @PostMapping("stop/{id}")
    fun stopBot(@PathVariable id: String) = botService.stopBot(id)

    @IsModerator
    @GetMapping("/active-bots")
    fun getAllActiveBots() = botService.getAllActiveBots()

    @IsModerator
    @PostMapping("add-bot/{id}")
    fun addBot(@PathVariable id: String) = botService.addBotToOperator(id)

    @IsModerator
    @PostMapping("remove-bot/{id}")
    fun removeBot(@PathVariable id: String) = botService.removeBotFromOperator(id)
}

@RestController
@RequestMapping("sessions")
class SessionController(private val sessionService: SessionService) {

    @IsModerator
    @GetMapping
    fun getAllSessions(pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSession(pageable)
    }

    @IsModerator
    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long): SessionInfo {
        return sessionService.getOne(id)
    }

    @IsModerator
    @GetMapping("user/{userId}")
    fun getAllSessionUser(@PathVariable userId: Long, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSessionUser(userId, pageable)
    }

    @IsModerator
    @GetMapping("operator/{operatorId}")
    fun getAllSessionOperator(@PathVariable operatorId: Long, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSessionOperator(operatorId, pageable)
    }


    @IsModerator
    @PostMapping("user/{userId}")
    fun getAllSessionUserDateRange(
        @PathVariable userId: Long, @RequestBody dto: DateRangeRequest, pageable: Pageable
    ): Page<SessionInfo> {
        return sessionService.getAllSessionUserDateRange(userId, dto, pageable)
    }

    @IsModerator
    @PostMapping("operator/{operatorId}")
    fun getAllSessionOperatorDateRange(
        @PathVariable operatorId: Long, @RequestBody dto: DateRangeRequest, pageable: Pageable
    ): Page<SessionInfo> {
        return sessionService.getAllSessionOperatorDateRange(operatorId, dto, pageable)
    }

    @IsModerator
    @GetMapping("status")
    fun getSessionsByStatus(@RequestParam status: SessionStatusEnum, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getSessionsByStatus(status, pageable)
    }

    @IsModerator
    @GetMapping("operators/high-rate")
    fun getHighRateOperator(pageable: Pageable): Page<RateInfo> {
        return sessionService.getHighRateOperator(pageable)
    }


    @IsModerator
    @GetMapping("operators/low-rate")
    fun getLowRateOperator(pageable: Pageable): Page<RateInfo> {
        return sessionService.getLowRateOperator(pageable)
    }

    @IsModerator
    @PostMapping("operators/high-rate")
    fun getHighRateOperatorDateRange(
        @RequestBody dto: DateRangeRequest, pageable: Pageable
    ): Page<RateInfo> {
        return sessionService.getHighRateOperatorDateRange(dto, pageable)
    }


    @IsModerator
    @PostMapping("operators/low-rate")
    fun getLowRateOperatorDateRange(
        @RequestBody dto: DateRangeRequest, pageable: Pageable
    ): Page<RateInfo> {
        return sessionService.getLowRateOperatorDateRange(dto, pageable)
    }

    @IsModerator
    @GetMapping("operators/rate/{operatorId}")
    fun getOperatorRate(@PathVariable operatorId: Long, pageable: Pageable): Page<RateInfo> {
        return sessionService.getOperatorRate(operatorId, pageable)
    }
}


@RestController
@RequestMapping("private/manage-users")
class PrivateUserController(
    private val userService: UserService, private val sessionService: SessionService
) {
    @IsModerator
    @GetMapping("get-users")
    fun getUsers() = userService.getAllUsers()

    @IsModerator
    @DeleteMapping("delete-user/{userId}")
    fun deleteUser(@PathVariable userId: Long) = userService.deleteUser(userId)

    @IsModerator
    @GetMapping("get-sessions-of-user/{userId}")
    fun getAllSessionUser(@PathVariable userId: Long, pageable: Pageable) =
        sessionService.getAllSessionUser(userId, pageable)

    @IsModerator
    @GetMapping("get-user/{id}")
    fun getUserById(@PathVariable id: Long) = userService.getUserById(id)
}

@RestController
@RequestMapping("operator")
class OperatorController(
    private val messageToOperatorService: MessageToOperatorService,
) {
    @IsModerator
    @GetMapping("get-sessions")
    fun getSessions() = messageToOperatorService.getSessions()

    @IsModerator
    @GetMapping("get-session-messages/{id}")
    fun getSessionMessages(@PathVariable id: String) = messageToOperatorService.getSessionMessages(id)

    @IsUser
    @GetMapping("get-unread-messages/{id}")
    fun getUnreadMessages(@PathVariable id: String) = messageToOperatorService.getUnreadMessages(id)

    @IsModerator
    @PostMapping("/send-msg")
    fun sendMessage(message: OperatorSentMsgRequest) =
        messageToOperatorService.sendMessage(message)
}

@RestController
@RequestMapping("bot-fileinfo")
class FileInfoController(
    private val fileInfoService: FileInfoService
){
    @PostMapping("upload")
    fun upload(@RequestParam("file") multipartFile: MultipartFile) = fileInfoService.upload(multipartFile)
}
