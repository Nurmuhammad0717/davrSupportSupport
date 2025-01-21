package uz.davrmobile.support.dto

import uz.davrmobile.support.enm.AppealSection
import java.util.Date

@Suppress("ImportOrdering")
data class ResultUnboundMessagesDto(
    val appealSection: AppealSection?,
    val open: Boolean?,
    val username: String?,
    val chatUid: String?,
    val clientId: String?,
    val type: String?,
    val fileId: String?,
    val text: String?,
    val createdDate: Date?,
    val unread: Int?,
    val fileName: String?,
    val fileSize: String?,
)
