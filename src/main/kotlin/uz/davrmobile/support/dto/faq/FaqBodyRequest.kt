package uz.davrmobile.support.dto.faq

import uz.davrmobile.support.entity.LocalizedString

data class FaqBodyRequest(
    val title: LocalizedString,
    val texts: List<LocalizedString>?
)
