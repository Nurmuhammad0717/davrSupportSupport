package uz.davrmobile.support.repository

import javax.persistence.EntityManager
import javax.transaction.Transactional
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.findByIdOrNull
import uz.davrmobile.support.entity.BaseEntity

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb ->
        cb.equal(root.get<Boolean>("deleted"), false)
    }

    override fun findByIdAndDeletedFalse(id: Long): T? =
        findByIdOrNull(id)
            ?.run {
                if (deleted) {
                    null
                } else {
                    this
                }
            }

    @Transactional
    override fun trash(id: Long): T? =
        findByIdOrNull(id)
            ?.run {
                deleted = true
                save(this)
            }

    override fun findAllNotDeleted(): List<T> =
        findAll(isNotDeletedSpecification)

    override fun findAllNotDeleted(pageable: Pageable): List<T> =
        findAll(isNotDeletedSpecification, pageable)
            .content
}
