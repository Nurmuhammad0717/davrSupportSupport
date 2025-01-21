package uz.davrmobile.support.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import uz.davrmobile.support.dto.faq.FaqRequest
import uz.davrmobile.support.dto.faq.FaqResponse
import uz.davrmobile.support.enm.FaqType

@Entity
data class FaqEntity(
    val category: LocalizedString,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(name = "faq_entity_and_body")
    val bodyItems: List<FaqEntityBody>,
    val orderId: Int,
    @Enumerated(EnumType.STRING) val type: FaqType
) : BaseEntity() {

    fun toRequest(): FaqRequest {
        val bodyItemsDto = bodyItems.map { it.toRequest() }
        return FaqRequest(category, bodyItemsDto, orderId, type)
    }

    fun toResponse(): FaqResponse {
        val bodyItemsDto = bodyItems.map { it.toResponse() }
        return FaqResponse(category.localized(), bodyItemsDto, id!!)
    }
}
