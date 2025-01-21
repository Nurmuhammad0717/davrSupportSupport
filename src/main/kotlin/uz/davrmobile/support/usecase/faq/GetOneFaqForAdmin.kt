package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.faq.FaqRequest
import uz.davrmobile.support.exception.ObjectNotFoundException
import uz.davrmobile.support.repository.FaqRepository

@Service
class GetOneFaqForAdmin(
    private val repository: FaqRepository
) {
    fun execute(id: Long): FaqRequest {
        return repository.findByIdAndDeletedFalse(id)?.toRequest() ?: throw ObjectNotFoundException("FAQ", id)
    }
}
