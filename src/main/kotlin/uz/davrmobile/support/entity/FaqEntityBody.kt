package uz.davrmobile.support.entity

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import uz.davrmobile.support.dto.faq.FaqBodyRequest
import uz.davrmobile.support.dto.faq.FaqBodyResponse

@Entity
data class FaqEntityBody(
    val title: LocalizedString,
    @ElementCollection
    @Column(columnDefinition = "TEXT")
    val texts: List<LocalizedString>?
) : BaseEntity() {

    fun toRequest(): FaqBodyRequest {
        return FaqBodyRequest(title, texts)
    }

    fun toResponse(): FaqBodyResponse {
        return FaqBodyResponse(title.localized(), texts?.map { it.localized() })
    }
}
