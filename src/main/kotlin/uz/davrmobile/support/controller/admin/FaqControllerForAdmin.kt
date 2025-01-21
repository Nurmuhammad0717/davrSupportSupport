package uz.davrmobile.support.controller.admin

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uz.davrmobile.support.dto.BaseResponse
import uz.davrmobile.support.dto.faq.FaqRequest
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.enm.FaqType
import uz.davrmobile.support.usecase.faq.CreateFaqUseCase
import uz.davrmobile.support.usecase.faq.DeleteFaqUseCase
import uz.davrmobile.support.usecase.faq.GetAllFaqUseCase
import uz.davrmobile.support.usecase.faq.GetOneFaqForAdmin
import uz.davrmobile.support.usecase.faq.UpdateFaqUseCase

@RestController
@RequestMapping("/admin/faq")
@PreAuthorize("hasAnyAuthority('MODERATOR','DEV', 'SUPPORT', 'CALL_CENTER')")
class FaqControllerForAdmin(
    private val createFaqUseCase: CreateFaqUseCase,
    private val updateFaqUseCase: UpdateFaqUseCase,
    private val deleteFaqUseCase: DeleteFaqUseCase,
    private val getAllFaqUseCase: GetAllFaqUseCase,
    private val getOneFaqForAdmin: GetOneFaqForAdmin
) {

    @PostMapping
    fun add(@RequestBody request: FaqRequest): FaqResponse = createFaqUseCase.execute(request)

    @PutMapping("/{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: FaqRequest): FaqResponse {
        return updateFaqUseCase.execute(id, request)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): BaseResponse = deleteFaqUseCase.execute(id)

    @GetMapping
    fun getAll(@RequestParam type: FaqType?): List<FaqResponse> = getAllFaqUseCase.execute(type)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): FaqRequest = getOneFaqForAdmin.execute(id)
}
