package uz.davrmobile.support.dto.faq

import uz.davrmobile.support.enm.FaqType
import uz.davrmobile.support.entity.LocalizedString

data class FaqRequest(
    val category: LocalizedString,
    val bodyItems: List<FaqBodyRequest>,
    val order: Int,
    val type: FaqType
)
