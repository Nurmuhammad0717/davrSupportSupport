package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import uz.davrmobile.support.bot.bot.BotService
import uz.davrmobile.support.util.IsAdmin
import uz.davrmobile.support.util.IsModerator
import uz.davrmobile.support.util.IsUser
import java.util.*
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
    fun getAll() = botService.getAllBots()

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
@RequestMapping("operator")
class OperatorController(
    private val messageToOperatorService: MessageToOperatorService,
) {
    @IsModerator
    @GetMapping("get-sessions")
    fun getSessions(@RequestBody @Valid request: GetSessionRequest, pageable: Pageable) =
        messageToOperatorService.getSessions(request, pageable)

    @IsModerator
    @GetMapping("get-session-messages/{id}")
    fun getSessionMessages(@PathVariable id: String) = messageToOperatorService.getSessionMessages(id)

    @IsModerator
    @GetMapping("get-unread-messages/{id}")
    fun getUnreadMessages(@PathVariable id: String) = messageToOperatorService.getUnreadMessages(id)

    @IsModerator
    @PostMapping("/send-msg")
    fun sendMessage(@RequestBody message: OperatorSentMsgRequest) = messageToOperatorService.sendMessage(message)

    @IsModerator
    @PostMapping("/end-session/{sessionId}")
    fun closeSession(@PathVariable sessionId: String) = messageToOperatorService.closeSession(sessionId)
}

@RestController
@RequestMapping("bot-fileinfo")
class FileInfoController(
    private val fileInfoService: FileInfoService
) {
    @IsModerator
    @PostMapping("upload")
    fun upload(@RequestParam("file") multipartFile: MutableList<MultipartFile>) = fileInfoService.upload(multipartFile)

    @IsModerator
    @GetMapping("download/{hash-id}")
    fun download(@PathVariable("hash-id") hashId: String, response: HttpServletResponse) =
        fileInfoService.download(hashId, response)

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
){

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
){
    @GetMapping
    fun getSessionInfoByOperator(
        @RequestParam("startDate") startDate: Date,
        @RequestParam("endDate") endDate: Date,
        @RequestParam("operatorId") operatorId: Long): SessionInfoByOperator = statisticService.getSessionByOperator(operatorId, startDate, endDate)
}