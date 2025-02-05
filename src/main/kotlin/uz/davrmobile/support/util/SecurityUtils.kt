package uz.davrmobile.support.util

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import uz.davrmobile.support.enm.UserRole

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyAuthority('MODERATOR','DEV','ADMIN')")
annotation class IsAdmin

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyAuthority('MODERATOR','DEV', 'SUPPORT', 'CALL_CENTER')")
annotation class IsModerator

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyAuthority('USER','SUPPORT', 'CALL_CENTER')")
annotation class IsUser


fun userId(): Long =
    SecurityContextHolder.getContext().getUserId()

fun phoneNumber(): String =
    SecurityContextHolder.getContext().getPhone()

fun phoneNumberAsUsername(): String {
    return ""
}


fun SecurityContext.getUserId(): Long {
    return 1
}

fun SecurityContext.getPhone(): String {
    return ""
}

//fun SecurityContext.getRoles(): List<UserRole> {
//    var roles = emptyList<String>()
//    if (authentication is OAuth2Authentication) {
//        val authentication = authentication as OAuth2Authentication
//        roles = authentication.authorities.map { it.authority }
//    }
//    return roles.map { UserRole.valueOf(it) }
//}

fun SecurityContext.getDetail(key: String): Any? {
    if (authentication is OAuth2Authentication) {
        val details = (authentication as OAuth2Authentication).userAuthentication.details as Map<*, *>
        return details[key]
    }
    return null
}

fun SecurityContext.getPermissions() = getDetail("permissions")?.run {
    val permissions = this as List<*>
    permissions.map { it as String }
} ?: listOf()

fun SecurityContext.getPermissionDetails() = getDetail("details")?.let {
    val details = it as Map<*, *>
    val permissionDetails = mutableMapOf<String, List<String>>()
    details.forEach { (key, value) ->
        permissionDetails[key as String] = (value as List<*>).map { v -> (v as String) }
    }
    permissionDetails
} ?: mapOf()

fun SecurityContext.getRoles(): List<UserRole> {
    return UserRole.values().toList()
}

fun permissions() = SecurityContextHolder.getContext().getPermissions()
fun permissionDetails() = SecurityContextHolder.getContext().getPermissionDetails()
fun roles() = SecurityContextHolder.getContext().getRoles()
