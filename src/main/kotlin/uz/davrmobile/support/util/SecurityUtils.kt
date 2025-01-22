package uz.davrmobile.support.util

import org.springframework.security.access.prepost.PreAuthorize
import uz.davrmobile.support.enm.UserRole

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyAuthority('MODERATOR','DEV', 'SUPPORT', 'CALL_CENTER')")
annotation class IsModerator

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyAuthority('USER','SUPPORT', 'CALL_CENTER')")
annotation class IsUser


fun userId(): Long =
    getUserId()

fun phoneNumber(): String =
    getPhone()

fun phoneNumberAsUsername(): String {
    return phoneNumber()
}


fun getUserId(): Long {
    return 1
}

fun getPhone(): String {
    return ""
}

fun getRoles(): List<UserRole> {
    return UserRole.values().toList()
}
