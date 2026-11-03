package digdaserver.domain.highlight.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 그룹별 월간 하이라이트("지난달의 디그팟").
 *
 * 매월 1일 00:00(KST) 스케줄러가 지난달 일기를 집계해 한 번만 저장하고, 앱은 저장된 결과를 조회한다.
 * (group_room_id, target_month) 유니크 제약이 중복 생성(재기동·다중 인스턴스·재시도)을 막는 최종 방어선이다.
 * 행은 집계·저장이 모두 끝난 뒤 한 트랜잭션으로만 insert 되므로 존재 = 생성 완료([status] = COMPLETED).
 *
 * group_room 과는 FK 없이 id 만 보관한다 — 그룹 영구 삭제 시 GroupRoomChildPurger 가 함께 정리한다.
 * prod 는 SchemaAutoMigration 에서 동일 테이블을 멱등 CREATE.
 */
@Entity
@Table(
    name = "monthly_highlight",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_monthly_highlight_group_month", columnNames = ["group_room_id", "target_month"])
    ]
)
class MonthlyHighlight(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_highlight_id")
    val id: Long = 0L,

    @Column(name = "group_room_id", nullable = false)
    val groupRoomId: Long,

    /** 집계 대상 월, "yyyy-MM" (예: "2026-10"). */
    @Column(name = "target_month", nullable = false, length = 7)
    val targetMonth: String,

    @Column(nullable = false, length = 16)
    val status: String = STATUS_COMPLETED,

    @Column(name = "total_diary_count", nullable = false)
    val totalDiaryCount: Int,

    @Column(name = "cover_image_url", length = 500)
    val coverImageUrl: String? = null,

    /** 통계·대표 일기·요약 등 화면 콘텐츠 전체(JSON). 스키마 변경 없이 항목을 늘릴 수 있도록 JSON 으로 둔다. */
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "generated_at", nullable = false)
    val generatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
