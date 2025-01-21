package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.faq.FaqRequest
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.entity.FaqEntityBody
import uz.davrmobile.support.exception.ObjectNotFoundException
import uz.davrmobile.support.repository.FaqRepository

@Service
class UpdateFaqUseCase(
    private val repository: FaqRepository
) {
    fun execute(id: Long, request: FaqRequest): FaqResponse {
        val faq = repository.findByIdAndDeletedFalse(id) ?: throw ObjectNotFoundException("FAQ", id)

        val updatedFaq = faq.copy(
            category = request.category,
            bodyItems = request.bodyItems.map { FaqEntityBody(it.title, it.texts) },
            orderId = request.order
        ).apply {
            this.id = faq.id
            createdBy = faq.createdBy
            createdDate = faq.createdDate
        }

        return repository.save(updatedFaq).toResponse()
    }
}
