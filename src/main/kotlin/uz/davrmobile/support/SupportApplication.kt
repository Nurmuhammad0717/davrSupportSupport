package uz.davrmobile.support

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import uz.davrmobile.support.repository.BaseRepositoryImpl

@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
@Suppress("SpreadOperator")
class SupportApplication

fun main(args: Array<String>) {
    runApplication<SupportApplication>(*args)
}