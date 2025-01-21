package uz.davrmobile.support.entity

import uz.davrmobile.support.dto.MessageModel
import uz.davrmobile.support.enm.MessageType
import uz.davrmobile.support.enm.SenderType
import javax.persistence.*

@Entity
data class Message(
    @Column(columnDefinition = "TEXT") val text: String?,
    val senderId: Long,
    @Enumerated(EnumType.STRING)
    val messageType: MessageType,
    @Enumerated(EnumType.STRING)
    val senderType: SenderType,
    @ManyToOne(fetch = FetchType.LAZY)
    val chat: Chat,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val read: Boolean = false,
) : BaseEntity() {

    fun toMessageModel(): MessageModel =
        MessageModel(
            messageId = id,
            text = text,
            fileId = fileId,
            fileName = fileName,
            fileSize = fileSize,
            read = read,
            createdTime = createdDate!!.time,
            senderType = senderType,
            messageType = messageType,
        )
}
