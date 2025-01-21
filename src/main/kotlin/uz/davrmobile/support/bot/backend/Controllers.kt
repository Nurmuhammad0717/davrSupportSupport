package uz.davrmobile.support.bot.backend

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import uz.davrmobile.support.bot.bot.BotService

@RestController
@RequestMapping("/bot")
class BotController(private val botService: BotService) {
    @PostMapping
    fun create(@RequestBody req: TokenRequest) = botService.createBot(req)

    @GetMapping
    fun getAll() = botService.getAllBots()

    @GetMapping("{id}")
    fun getOneBot(@PathVariable id: Long) = botService.getOneBot(id)

    @DeleteMapping("{id}")
    fun deleteBot(@PathVariable id: Long) = botService.deleteBot(id)

    @GetMapping("/active-bots")
    fun getAllActiveBots() = botService.getAllActiveBots()

}

@RestController
@RequestMapping("sessions")
class SessionController(private val sessionService: SessionService) {

    @GetMapping
    fun getAllSessions(pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSession(pageable)
    }

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long): SessionInfo {
        return sessionService.getOne(id)
    }

    @GetMapping("user/{userId}")
    fun getAllSessionUser(@PathVariable userId: Long, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSessionUser(userId, pageable)
    }

    @GetMapping("operator/{operatorId}")
    fun getAllSessionOperator(@PathVariable operatorId: Long, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getAllSessionOperator(operatorId, pageable)
    }


    @PostMapping("user/{userId}")
    fun getAllSessionUserDateRange(
        @PathVariable userId: Long,
        @RequestBody dto: DateRangeDTO,
        pageable: Pageable
    ): Page<SessionInfo> {
        return sessionService.getAllSessionUserDateRange(userId, dto, pageable)
    }

    @PostMapping("operator/{operatorId}")
    fun getAllSessionOperatorDateRange(
        @PathVariable operatorId: Long,
        @RequestBody dto: DateRangeDTO,
        pageable: Pageable
    ): Page<SessionInfo> {
        return sessionService.getAllSessionOperatorDateRange(operatorId, dto, pageable)
    }

    @GetMapping("status")
    fun getSessionsByStatus(@RequestParam status: SessionStatusEnum, pageable: Pageable): Page<SessionInfo> {
        return sessionService.getSessionsByStatus(status, pageable)
    }

    @GetMapping("operators/high-rate")
    fun getHighRateOperator(pageable: Pageable): Page<RateInfo> {
        return sessionService.getHighRateOperator(pageable)
    }

    @GetMapping("operators/low-rate")
    fun getLowRateOperator(pageable: Pageable): Page<RateInfo> {
        return sessionService.getLowRateOperator(pageable)
    }

    @PostMapping("operators/high-rate")
    fun getHighRateOperatorDateRange(
        @RequestBody dto: DateRangeDTO,
        pageable: Pageable
    ): Page<RateInfo> {
        return sessionService.getHighRateOperatorDateRange(dto, pageable)
    }

    @PostMapping("operators/low-rate")
    fun getLowRateOperatorDateRange(
        @RequestBody dto: DateRangeDTO,
        pageable: Pageable
    ): Page<RateInfo> {
        return sessionService.getLowRateOperatorDateRange(dto, pageable)
    }

    @GetMapping("operators/rate/{operatorId}")
    fun getOperatorRate(@PathVariable operatorId: Long, pageable: Pageable): Page<RateInfo> {
        return sessionService.getOperatorRate(operatorId, pageable)
    }
}


@RestController
@RequestMapping("private/manage-users")
class PrivateUserController(
    private val userService: UserService,
    private val sessionService: SessionService
) {
    @GetMapping("get-users")
    fun getUsers() = userService.getAllUsers()

    @DeleteMapping("delete-user/{userId}")
    fun deleteUser(@PathVariable userId: Long) = userService.deleteUser(userId)

    @GetMapping("get-sessions-of-user/{userId}")
    fun getAllSessionUser(@PathVariable userId: Long, pageable: Pageable) =
        sessionService.getAllSessionUser(userId, pageable)

    @GetMapping("get-user/{id}")
    fun getUserById(@PathVariable id: Long) = userService.getUserById(id)
}

@RestController
@RequestMapping("operator")
class OperatorController {

}