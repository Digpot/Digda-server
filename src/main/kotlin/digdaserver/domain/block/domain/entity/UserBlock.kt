package digdaserver.domain.block.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 사용자 단위 차단(단방향·전역). [blocker] 의 화면에서 [blocked] 의 모든 일기·댓글·일정이
 * 숨겨진다. 양방향이 아니므로 차단당한 쪽은 영향을 받지 않는다.
 *
 * 차단은 콘텐츠를 삭제하지 않는다 — 집계(시그니처 지도 색칠)·하루 1편 슬롯은 그대로 유지되고,
 * 조회 시점에 숨김 처리만 한다.
 */
@Entity
@Table(
    name = "user_block",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_block", columnNames = ["blocker_id", "blocked_id"])
    ]
)
class UserBlock(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_block_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    val blocker: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    val blocked: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
