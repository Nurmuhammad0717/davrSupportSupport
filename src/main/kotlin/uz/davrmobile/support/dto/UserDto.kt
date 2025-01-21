package uz.davrmobile.support.dto

data class UserDto(
    val id: Long,
    val phoneNumber: String,
    val hashId: String,
    val fullName: String?
)
