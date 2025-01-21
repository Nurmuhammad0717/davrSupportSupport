package uz.davrmobile.support.dto.faq

data class FaqResponse(
    val category: String,
    val bodyItems: List<FaqBodyResponse>,
    val id: Long
)
