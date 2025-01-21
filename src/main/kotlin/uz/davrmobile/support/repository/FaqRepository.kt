package uz.davrmobile.support.repository

import org.springframework.stereotype.Repository
import uz.davrmobile.support.enm.FaqType
import uz.davrmobile.support.entity.FaqEntity

@Repository
interface FaqRepository : BaseRepository<FaqEntity> {
    fun findAllByDeletedFalseOrderByOrderId(): List<FaqEntity>
    fun findAllByTypeAndDeletedFalseOrderByOrderId(type: FaqType): List<FaqEntity>
}
