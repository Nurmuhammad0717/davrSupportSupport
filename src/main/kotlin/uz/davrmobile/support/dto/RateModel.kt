package uz.davrmobile.support.dto

import org.hibernate.validator.constraints.Range

data class RateModel(
    @field: Range(min = 0, max = 5)
    val rate: Short,
)
