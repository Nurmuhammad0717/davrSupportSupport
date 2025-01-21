package uz.davrmobile.support.usecase

import org.springframework.stereotype.Component
import uz.davrmobile.support.dto.SectionDto
import uz.davrmobile.support.enm.AppealSection

@Component
class GetAllSectionsUseCase {

    companion object {
        val ALL_SECTIONS = AppealSection.values()
            .map { SectionDto(it.name) }
    }

    fun execute(): List<SectionDto> =
        ALL_SECTIONS
}
