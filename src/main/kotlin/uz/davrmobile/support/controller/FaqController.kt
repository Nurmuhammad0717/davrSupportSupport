package uz.davrmobile.support.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.enm.FaqType
import uz.davrmobile.support.usecase.faq.GetAllFaqUseCase
import uz.davrmobile.support.usecase.faq.GetOneFaqUseCase

@RestController
@RequestMapping("/faq")
class FaqController(
    private val getAllFaqUseCase: GetAllFaqUseCase,
    private val getOneFaqUseCase: GetOneFaqUseCase
) {
    @GetMapping
    fun getAll(@RequestParam type: FaqType?): List<FaqResponse> = getAllFaqUseCase.execute(type)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): FaqResponse = getOneFaqUseCase.execute(id)
}
