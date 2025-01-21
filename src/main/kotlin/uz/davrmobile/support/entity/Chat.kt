package uz.davrmobile.support.entity

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import uz.davrmobile.support.enm.AppealSection

@Entity
data class Chat(
    @Enumerated(EnumType.STRING)
    val appealSection: AppealSection,
    val chatUID: String,
    val clientId: Long,
    val clientUsername: String,
    val moderatorId: Long? = null,
    val moderatorUsername: String? = null,
    val moderatorName: String? = null,
    val rate: Short? = null,
    val open: Boolean = true,
    val moderUid: String? = null,
    val clientUid: String? = null,
) : BaseEntity()
