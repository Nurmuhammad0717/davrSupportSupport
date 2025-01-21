package uz.davrmobile.support.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import uz.davrmobile.support.entity.BaseEntity

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    fun findByIdAndDeletedFalse(id: Long): T?

    fun trash(id: Long): T?

    fun findAllNotDeleted(): List<T>

    fun findAllNotDeleted(pageable: Pageable): List<T>
}
