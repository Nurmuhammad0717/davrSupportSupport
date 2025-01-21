package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.enm.FaqType
import uz.davrmobile.support.repository.FaqRepository

@Service
class GetAllFaqUseCase(
    private val repository: FaqRepository
) {
    fun execute(type: FaqType?): List<FaqResponse> {
        return if (type != null) {
            repository.findAllByTypeAndDeletedFalseOrderByOrderId(type).map { it.toResponse() }
        } else {
            repository.findAllByDeletedFalseOrderByOrderId().map { it.toResponse() }
        }
    }
}
