package digdaserver.domain.report.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 사용자 신고 기록. 어드민 검토용으로만 쌓이며, 신고 자체가 콘텐츠를 지우지는 않는다.
 * (신고하면 신고자 본인에게서만 자동 숨김 처리 — ContentHide 가 별도로 생성된다.)
 *
 * targetId 는 종류에 따라 Long PK 또는 UUID 문자열이라 String 으로 보관한다.
 */
@Entity
@Table(
    name = "report",
    indexes = [
        Index(name = "idx_report_status", columnList = "status"),
        Index(name = "idx_report_target", columnList = "target_type, target_id")
    ]
)
class Report(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    val targetType: ReportTargetType,

    @Column(name = "target_id", nullable = false, length = 64)
    val targetId: String,

    /** 신고 대상이 속한 그룹방(있으면). USER 신고나 그룹 외 대상은 null. 어드민 추적용. */
    @Column(name = "group_room_id")
    val groupRoomId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val reason: ReportReason,

    @Column(name = "detail", length = 500)
    val detail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null
) {
    fun markReviewed(status: ReportStatus) {
        this.status = status
        this.reviewedAt = LocalDateTime.now()
    }
}
