package uz.davrmobile.support.config.security

//import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory
//import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
//import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
//import org.springframework.security.oauth2.client.OAuth2ClientContext
//import org.springframework.security.oauth2.client.OAuth2RestTemplate
//import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter

@EnableResourceServer
@Configuration
class ResourceServerConfig : ResourceServerConfigurerAdapter() {
    override fun configure(http: HttpSecurity?) {
        http
            ?.headers()?.cacheControl()?.disable()?.and()
            ?.sessionManagement()?.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            ?.and()
            ?.authorizeRequests()
            ?.antMatchers(
                "/internal/**",
                "/faq/**",
                "/configuration/ui",
                "/configuration/**",
                "/configuration/security",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/**",
                "/webjars/**",


                "/bot/**",
                "/operator/**",
                "/bot-fileinfo/**",
            )?.permitAll()
            ?.antMatchers("/**")?.authenticated()
            ?.and()
            ?.csrf()?.disable()
    }

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

//    @Bean
//    fun feignInterceptor(context: OAuth2ClientContext, details: OAuth2ProtectedResourceDetails) =
//        OAuth2FeignRequestInterceptor(context, details)

//    @Bean
//    fun oauth2RestTemplate(userInfoRestTemplateFactory: UserInfoRestTemplateFactory): OAuth2RestTemplate {
//        return userInfoRestTemplateFactory.userInfoRestTemplate
//    }
}
