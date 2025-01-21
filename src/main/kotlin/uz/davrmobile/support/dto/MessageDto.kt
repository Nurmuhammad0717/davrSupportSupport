package uz.davrmobile.support.dto

data class MessageDto(
    val text: String?,
    val fileId: String?,
    val fileName: String?,
    val fileSize: String?,
) {
    fun toDto(): MessageDto = MessageDto(
        text = text,
        fileId = fileId,
        fileName = fileName,
        fileSize = fileSize
    )
}
