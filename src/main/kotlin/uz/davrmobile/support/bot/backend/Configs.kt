package uz.davrmobile.support.bot.backend

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*

@Configuration
class Configs{
    @Bean
    fun messageSource(): ResourceBundleMessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasenames("bot_messages")
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setFallbackToSystemLocale(false)
        messageSource.setUseCodeAsDefaultMessage(true)
        return messageSource
    }
}
