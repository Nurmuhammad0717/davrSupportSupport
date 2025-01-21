package uz.davrmobile.support.dto

import uz.davrmobile.support.enm.AppealSection
import uz.davrmobile.support.enm.MessageType

data class SupportResultQueryDto(
    val appealSection: AppealSection? = null,
    val open: Boolean? = null,
    val chatUid: String? = null,
    val clientId: String? = null,
    val type: MessageType? = null,
    val fileId: String? = null,
    val text: String? = null,
    val createdDate: Long? = null,
    val unread: Int? = null,
    val clientUsername: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
)
