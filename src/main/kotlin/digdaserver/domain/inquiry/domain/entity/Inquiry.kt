package digdaserver.domain.inquiry.domain.entity

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
 * 고객센터 문의. 사용자가 마이페이지에서 작성하며 어드민이 검토한다.
 * 무분별한 작성을 막기 위해 작성 API 에서 하루 2건으로 제한한다(서비스 레이어에서 검사).
 */
@Entity
@Table(
    name = "inquiry",
    indexes = [
        Index(name = "idx_inquiry_user_created", columnList = "user_id, created_at"),
        Index(name = "idx_inquiry_status", columnList = "status")
    ]
)
class Inquiry(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "content", nullable = false, length = 1000)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: InquiryStatus = InquiryStatus.PENDING,

    /** 어드민 답변 내용. 미답변이면 null. */
    @Column(name = "answer", length = 2000)
    var answer: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "answered_at")
    var answeredAt: LocalDateTime? = null
) {
    /** 어드민이 답변을 등록 — 답변 내용 저장 + 상태 ANSWERED 전이. */
    fun answer(answer: String) {
        this.answer = answer
        this.status = InquiryStatus.ANSWERED
        this.answeredAt = LocalDateTime.now()
    }
}
