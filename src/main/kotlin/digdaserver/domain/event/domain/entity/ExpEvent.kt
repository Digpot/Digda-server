package digdaserver.domain.event.domain.entity

import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 모찌 경험치 배수 이벤트 — 어드민이 켜고 끄는 **단일 행**(app_config 와 동일한 운영 방식).
 *
 * 추석·설날 같은 시즌 이벤트에 "경험치 2배" 를 걸기 위한 설정이다. 켜져 있고 기간 안이면
 * 모든 경험치 적립 경로(일기 작성 / 퀴즈 정답)에서 적립량에 [multiplier] 가 곱해진다.
 * 코인은 곱하지 않는다 — 코인은 [CoinGrant] 로 일괄 지급하는 별도 이벤트.
 *
 * 기간은 양쪽 모두 null 을 허용한다(= 그 방향 제한 없음). 시작만 지정하면 "그때부터 무기한",
 * 종료만 지정하면 "그때까지" 로 동작한다.
 */
@Entity
@Table(name = "exp_event")
class ExpEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exp_event_id")
    val id: Long = 0L,

    /** 이벤트 스위치. 꺼져 있으면 기간과 무관하게 배수 1.0. */
    @Column(nullable = false)
    var enabled: Boolean = false,

    /** 앱 배너에 노출할 문구. 예) "🌕 추석 맞이 모찌 경험치 2배!" */
    @Column(nullable = false, length = 100)
    var title: String = "",

    /** 경험치 배수. 1.0 = 이벤트 없음. 어드민 입력은 서비스 계층에서 [MIN_MULTIPLIER]..[MAX_MULTIPLIER] 로 clamp. */
    @Column(nullable = false)
    var multiplier: Double = 1.0,

    /** 시작 시각(포함). null = 시작 제한 없음. */
    @Column(name = "start_at")
    var startAt: LocalDateTime? = null,

    /** 종료 시각(미포함). null = 종료 제한 없음. */
    @Column(name = "end_at")
    var endAt: LocalDateTime? = null

) : BaseTimeEntity() {

    /** [now] 기준으로 배수가 실제 적용되는 상태인지. */
    fun isActiveAt(now: LocalDateTime): Boolean {
        if (!enabled) return false
        if (multiplier <= 1.0) return false
        startAt?.let { if (now.isBefore(it)) return false }
        endAt?.let { if (!now.isBefore(it)) return false }
        return true
    }

    /** [now] 기준 적용 배수. 비활성이면 1.0. */
    fun multiplierAt(now: LocalDateTime): Double = if (isActiveAt(now)) multiplier else 1.0

    /**
     * [baseExp] 에 [now] 기준 배수를 적용한 최종 경험치. 반올림하며, 배수가 1.0 이상이라
     * 결과가 원본보다 작아지는 일은 없다(방어적으로 coerceAtLeast).
     */
    fun applyTo(baseExp: Int, now: LocalDateTime): Int {
        if (baseExp <= 0) return baseExp
        val applied = Math.round(baseExp * multiplierAt(now)).toInt()
        return applied.coerceAtLeast(baseExp)
    }

    fun update(
        enabled: Boolean,
        title: String,
        multiplier: Double,
        startAt: LocalDateTime?,
        endAt: LocalDateTime?
    ) {
        this.enabled = enabled
        this.title = title
        this.multiplier = multiplier.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
        this.startAt = startAt
        this.endAt = endAt
    }

    companion object {
        const val MIN_MULTIPLIER: Double = 1.0

        /** 운영 실수(20배 등)로 레벨이 통째로 날아가는 것을 막는 상한. */
        const val MAX_MULTIPLIER: Double = 10.0
    }
}
