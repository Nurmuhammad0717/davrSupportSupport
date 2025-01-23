package uz.davrmobile.support.bot.backend

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

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
