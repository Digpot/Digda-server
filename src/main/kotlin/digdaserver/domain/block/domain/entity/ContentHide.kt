package digdaserver.domain.block.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

/**
 * 개별 콘텐츠 숨김(특정 사용자에게서만). "이 게시물 숨기기" 또는 신고 시 자동으로 생성된다.
 * 사용자 단위 차단은 [UserBlock] 로 따로 둔다.
 *
 * 숨김은 콘텐츠를 삭제하지 않는다 — 하루 1편 슬롯/지도 집계는 유지하고 조회 시 숨김만 한다.
 */
@Entity
@Table(
    name = "content_hide",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_content_hide", columnNames = ["user_id", "target_type", "target_id"])
    ],
    indexes = [
        Index(name = "idx_content_hide_user", columnList = "user_id, target_type")
    ]
)
class ContentHide(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_hide_id")
    val id: Long = 0L,

    /** 숨긴 주체(이 사용자에게서만 안 보인다). */
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    val targetType: HideTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val reason: HideReason,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
