package uz.davrmobile.support.dto

import uz.davrmobile.support.enm.MessageType

interface UserResultQuery {
    fun getappealSection(): String
    fun getopen(): Boolean
    fun getid(): Long
    fun getchatUid(): String
    fun getmoderatorName(): String
    fun getclientId(): String
    fun gettype(): MessageType
    fun getfileid(): String
    fun gettext(): String
    fun getcreatedDate(): String
    fun getunread(): Int
}
