package uz.davrmobile.support.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import uz.davrmobile.support.util.getUserId
import java.util.Optional

@Configuration
@EnableJpaAuditing
class EntityAuditingConfig {

    @Bean
    fun userIdAuditorAware(): AuditorAware<Long> =
        AuditorAware { Optional.ofNullable(SecurityContextHolder.getContext().getUserId()) }
}
