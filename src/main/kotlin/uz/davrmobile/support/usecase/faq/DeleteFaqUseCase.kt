package uz.davrmobile.support.usecase.faq

import org.springframework.stereotype.Service
import uz.davrmobile.support.dto.BaseResponse
import uz.davrmobile.support.exception.ObjectNotFoundException
import uz.davrmobile.support.repository.FaqRepository

@Service
class DeleteFaqUseCase(
    private val repository: FaqRepository
) {
    fun execute(id: Long): BaseResponse {
        repository.findByIdAndDeletedFalse(id) ?: throw ObjectNotFoundException("FAQ", id)
        repository.trash(id)
        return BaseResponse()
    }
}
