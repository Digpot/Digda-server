package digdaserver.domain.event.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 코인 일괄 지급 1회의 기록.
 *
 * 지급 자체는 group_character 를 벌크 UPDATE 해서 끝나므로 "언제·얼마를·누가·몇 그룹에"
 * 줬는지가 어디에도 남지 않는다. 중복 지급(추석 코인을 두 번 쏘는 사고)을 사람이 알아채려면
 * 이력이 필요해서 지급 때마다 한 행을 남긴다.
 *
 * 지급 단위는 사용자가 아니라 **그룹 캐릭터(모찌)** 다 — 코인은 그룹 공용 지갑이라
 * 그룹당 [amount] 만큼 들어간다.
 */
@Entity
@Table(name = "coin_grant")
class CoinGrant(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coin_grant_id")
    val id: Long = 0L,

    /** 그룹(모찌) 1개당 지급한 코인. */
    @Column(nullable = false)
    val amount: Int,

    /** 운영 메모. 예) "2026 추석 이벤트" */
    @Column(nullable = false, length = 200)
    val reason: String = "",

    /** 실제로 코인이 들어간 그룹 캐릭터 수. */
    @Column(name = "target_count", nullable = false)
    val targetCount: Int = 0,

    /** 지급과 함께 전체 공지 푸시를 보냈는지. */
    @Column(nullable = false)
    val notified: Boolean = false,

    /** 지급을 실행한 어드민(로그 식별자 = 이메일 local-part). 알 수 없으면 "-". */
    @Column(name = "granted_by", nullable = false, length = 64)
    val grantedBy: String = "-"

) : BaseTimeEntity()
