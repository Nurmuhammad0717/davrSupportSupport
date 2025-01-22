package uz.davrmobile.support.bot.backend

import org.springframework.context.annotation.Bean
import org.springframework.context.support.ResourceBundleMessageSource

@Bean
fun messageSource(): ResourceBundleMessageSource {
    val messageSource = ResourceBundleMessageSource()
    messageSource.setBasenames("messages")
    messageSource.setDefaultEncoding("UTF-8")
    messageSource.setFallbackToSystemLocale(false)
    messageSource.setUseCodeAsDefaultMessage(true)
    return messageSource
}