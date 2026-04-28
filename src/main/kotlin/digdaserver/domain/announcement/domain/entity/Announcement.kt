package digdaserver.domain.announcement.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "announcement",
    indexes = [
        Index(name = "idx_announcement_created_at", columnList = "created_at")
    ]
)
class Announcement(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    val id: Long = 0L,

    @Column(nullable = false, length = 100)
    val title: String,

    @Column(nullable = false, length = 1000)
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    val targetType: AnnouncementTarget,

    @Column(name = "recipient_count", nullable = false)
    val recipientCount: Int

) : BaseTimeEntity()
