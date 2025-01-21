package uz.davrmobile.support.dto

import uz.davrmobile.support.enm.MessageType
import uz.davrmobile.support.enm.SenderType

data class MessageModel(
    val messageId: Long?,
    val text: String?,
    val fileId: String?,
    val fileName: String?,
    val fileSize: String?,
    val read: Boolean,
    val createdTime: Long?,
    val senderType: SenderType,
    val messageType: MessageType,
)
