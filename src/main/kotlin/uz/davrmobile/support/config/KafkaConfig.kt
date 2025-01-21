package uz.davrmobile.support.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.support.converter.StringJsonMessageConverter

@EnableKafka
@Configuration
class KafkaConfig {

    @Bean
    fun stringMessageConvertor() = StringJsonMessageConverter()
}