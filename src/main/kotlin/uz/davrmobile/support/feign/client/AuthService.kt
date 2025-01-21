package uz.davrmobile.support.feign.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import uz.davrmobile.support.dto.UserDto
import uz.davrmobile.support.feign.interceptor.FeignAuth2TokenConfiguration

@FeignClient(
    name = "auth-service",
    url = "\${service.auth}",
    configuration = [FeignAuth2TokenConfiguration::class],
)
interface AuthService {
    @GetMapping("/user/internal/user-info")
    fun getUserInfo(): UserDto
}
