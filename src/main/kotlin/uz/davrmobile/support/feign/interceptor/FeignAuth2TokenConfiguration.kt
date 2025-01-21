package uz.davrmobile.support.feign.interceptor

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.oauth2.client.OAuth2ClientContext
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails

class FeignAuth2TokenConfiguration : RequestInterceptor {

    @Bean
    fun feignInterceptor(context: OAuth2ClientContext, details: OAuth2ProtectedResourceDetails): RequestInterceptor =
        OAuth2FeignRequestInterceptor(context, details)

    override fun apply(template: RequestTemplate?) {
        template?.header("hl", LocaleContextHolder.getLocale().language)
    }
}