package uz.davrmobile.support.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import uz.davrmobile.support.util.userId
import java.util.*

@Configuration
@EnableJpaAuditing
class EntityAuditingConfig {

    @Bean
    fun userIdAuditorAware(): AuditorAware<Long> =
        AuditorAware { Optional.ofNullable(userId()) }
}
