package uz.davrmobile.support.entity

import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Date
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.Temporal
import javax.persistence.TemporalType

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    var createdDate: Date? = null,
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    var modifiedDate: Date? = null,
    @CreatedBy
    var createdBy: Long? = null,
    @LastModifiedBy
    var lastModifiedBy: Long? = null,
    @Column(nullable = false)
    @ColumnDefault(value = "false")
    var deleted: Boolean = false,
)
