package uz.davrmobile.support.bot.backend

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
class AppProperties {
         var username: String="admin"
         var fullName: String="admin"
         var phoneNumber: String="+998944310717"
}