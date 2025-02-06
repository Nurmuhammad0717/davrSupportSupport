package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import uz.davrmobile.support.bot.bot.BotService
import uz.davrmobile.support.util.IsModerator
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@RestController
@RequestMapping("/bot")
class BotController(private val botService: BotService) {
    //    @IsAdmin
    @IsModerator
    @PostMapping
    fun create(@RequestBody req: TokenRequest) = botService.createBot(req)

    @IsModerator
    @GetMapping
    fun getAll(pageable: Pageable, botStats: BotStatusEnum?) = botService.getAllBots(pageable, botStats)

    //    @IsAdmin
    @IsModerator
    @GetMapping("{id}")
    fun getOneBot(@PathVariable id: String) = botService.getOneBot(id)

    //    @IsAdmin
    @IsModerator
    @DeleteMapping("{id}")
    fun deleteBot(@PathVariable id: String) = botService.deleteBot(id)

    //    @IsAdmin
    @IsModerator
    @PostMapping("change-status")
    fun stopBot(@RequestParam id: String) = botService.changeStatus(id)

    @IsModerator
    @PostMapping("join/{id}")
    fun addBot(@PathVariable id: String) = botService.addBotToOperator(id)

    @IsModerator
    @PostMapping("leave/{id}")
    fun removeBot(@PathVariable id: String) = botService.removeBotFromOperator(id)
}

@RestController
@RequestMapping("operator")
class OperatorController(
    private val messageToOperatorService: MessageToOperatorService,
) {
    @IsModerator
    @PostMapping("sessions/my")
    fun getMySessions(pageable: Pageable) =
        messageToOperatorService.getMySessions(pageable)

    @IsModerator
    @PostMapping("sessions/waiting")
    fun getWaitingSessions(@RequestBody @Valid request: GetSessionRequest, pageable: Pageable) =
        messageToOperatorService.getWaitingSessions(request, pageable)

    @IsModerator
    @PostMapping("sessions/closed")
    fun getClosedSessions(pageable: Pageable) =
        messageToOperatorService.getClosedSessions(pageable)

    @IsModerator
    @PostMapping("take-session/{id}")
    fun takeSession(@PathVariable id: String) = messageToOperatorService.takeSession(id)

    @IsModerator
    @GetMapping("session-messages/{id}")
    fun getSessionMessages(@PathVariable id: String, pageable: Pageable) =
        messageToOperatorService.getSessionMessages(id, pageable)

    @IsModerator
    @GetMapping("unread-messages/{id}")
    fun getUnreadMessages(@PathVariable id: String, pageable: Pageable) =
        messageToOperatorService.getUnreadMessages(id, pageable)

    @IsModerator
    @PostMapping("send-msg")
    fun sendMessage(@RequestBody message: OperatorSentMsgRequest) = messageToOperatorService.sendMessage(message)

    @IsModerator
    @PostMapping("edit-msg")
    fun editMessage(@RequestBody message: OperatorEditMsgRequest) = messageToOperatorService.editMessage(message)

    @IsModerator
    @PostMapping("end-session/{sessionId}")
    fun closeSession(@PathVariable sessionId: String) = messageToOperatorService.closeSession(sessionId)

    @GetMapping("bots")
    fun getOperatorBots() = messageToOperatorService.getOperatorBots()
}

@RestController
@RequestMapping("bot-files")
class FileInfoController(
    private val fileInfoService: FileInfoService
) {
    @IsModerator
    @PostMapping("upload")
    fun upload(@RequestParam("file") multipartFile: MutableList<MultipartFile>) = fileInfoService.upload(multipartFile)

    @GetMapping("download/{hash-id}")
    fun download(@PathVariable("hash-id") hashId: String, response: HttpServletResponse) =
        fileInfoService.download(hashId, response)

    @IsModerator
    @GetMapping("show/{hash-id}")
    fun show(@PathVariable("hash-id") hashId: String, response: HttpServletResponse) =
        fileInfoService.show(hashId, response)

    @IsModerator
    @GetMapping("{hash-id}")
    fun find(@PathVariable("hash-id") hashId: String) = fileInfoService.find(hashId)

    //    @IsAdmin
    @IsModerator
    @GetMapping
    fun findAll(pageable: Pageable) = fileInfoService.findAll(pageable)
}

@RestController
@RequestMapping("standard-answers")
class StandardAnswerController(
    private val service: StandardAnswerService
) {
    @PostMapping
    fun create(@RequestBody request: StandardAnswerRequest) = service.create(request)

    @PutMapping("{id}")
    fun update(@RequestBody request: StandardAnswerUpdateRequest, @PathVariable id: Long) = service.update(request, id)

    @GetMapping("{id}")
    fun find(@PathVariable id: Long) = service.find(id)

    @GetMapping
    fun findAll(pageable: Pageable) = service.findAll(pageable)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("statistics")
class StatisticController(
    private val statisticService: StatisticService
) {
    @PostMapping("sessions")
    fun getSessionInfoByOperatorDateRange(@RequestBody request: OperatorStatisticRequest) =
        statisticService.getSessionByOperatorDateRange(request)
}

@RestController
@RequestMapping("users")
class BotUserController(private val userService: UserService) {
    @GetMapping
    fun getUsers(pageable: Pageable, @RequestParam fullName: String?) = userService.getAllUsers(pageable, fullName)
}