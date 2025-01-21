package uz.davrmobile.support.entity

import javax.persistence.Embeddable
import org.springframework.context.i18n.LocaleContextHolder

@Embeddable
data class LocalizedString(
    val uz: String,
    val ru: String,
    val en: String,
) {

    fun localized(): String {
        return when (LocaleContextHolder.getLocale().language) {
            "uz" -> this.uz
            "ru" -> this.ru
            "en" -> this.en
            else -> this.uz
        }
    }
}
