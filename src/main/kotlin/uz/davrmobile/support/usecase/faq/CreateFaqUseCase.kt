package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.faq.FaqRequest
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.entity.FaqEntity
import uz.davrmobile.support.entity.FaqEntityBody
import uz.davrmobile.support.repository.FaqRepository

@Service
class CreateFaqUseCase(
    private val repository: FaqRepository
) {
    fun execute(request: FaqRequest): FaqResponse {
        return repository.save(
            FaqEntity(
                category = request.category,
                bodyItems = request.bodyItems.map { FaqEntityBody(it.title, it.texts) },
                orderId = request.order,
                type = request.type
            )
        ).toResponse()
    }
}
