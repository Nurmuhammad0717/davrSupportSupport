package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.exception.ObjectNotFoundException
import uz.davrmobile.support.repository.FaqRepository

@Service
class GetOneFaqUseCase(
    private val repository: FaqRepository
) {
    fun execute(id: Long): FaqResponse {
        return repository.findByIdAndDeletedFalse(id)?.toResponse() ?: throw ObjectNotFoundException("FAQ", id)
    }
}
