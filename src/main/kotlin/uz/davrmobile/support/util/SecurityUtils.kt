package uz.davrmobile.support.util

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
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
    SecurityContextHolder.getContext().getUserId()!!

fun phoneNumber(): String =
    SecurityContextHolder.getContext().getPhone()

fun phoneNumberAsUsername(): String {
    return if (phoneNumber().contains(":")) {
        phoneNumber().split(":")[0]
    } else {
        phoneNumber()
    }
}


fun SecurityContext.getUserId(): Long? {
    if (authentication is OAuth2Authentication) {
        val details = (authentication as OAuth2Authentication).userAuthentication.details as Map<*, *>
        val userId = details["userId"]
        if (userId is Int) return userId.toLong()
        return userId as Long
    }
    return null
}

fun SecurityContext.getPhone(): String {
    if (authentication is OAuth2Authentication) {
        val details = (authentication as OAuth2Authentication).userAuthentication.details as Map<*, *>
        val phone = details["phoneNumber"] as String
        return if (phone.contains(":")) {
            phone.split(":")[0]
        } else {
            phone
        }
    }
    return ""
}

fun SecurityContext.getRoles(): List<UserRole> {
    var roles = emptyList<String>()
    if (authentication is OAuth2Authentication) {
        val authentication = authentication as OAuth2Authentication
        roles = authentication.authorities.map { it.authority }
    }
    return roles.map { UserRole.valueOf(it) }
}
