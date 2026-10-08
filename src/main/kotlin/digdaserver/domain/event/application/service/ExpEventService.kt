package digdaserver.domain.event.application.service

import digdaserver.domain.event.presentation.dto.res.ExpEventResponse
import java.time.LocalDateTime

/**
 * 모찌 경험치 배수 이벤트(시즌 이벤트) 도메인.
 *
 * 경험치를 적립하는 모든 지점(일기 작성 / 퀴즈 정답)은 적립 직전에 [boost] 로 최종 경험치를
 * 구한다. 설정은 단일 행이고 읽기 비용이 작아 별도 캐시는 두지 않는다.
 */
interface ExpEventService {

    /** 앱/어드민 공용 이벤트 상태 조회. 행이 없으면 [ExpEventResponse.default]. */
    fun get(): ExpEventResponse

    /**
     * [baseExp] 에 현재 배수를 적용한 결과. 이벤트가 없거나 기간 밖이면 배수 1.0 그대로 통과.
     * 0 이하의 baseExp 는 배수를 적용하지 않는다(위로 보상 0 같은 케이스).
     */
    fun boost(baseExp: Int, now: LocalDateTime = LocalDateTime.now()): ExpBoost

    /**
     * 배수 적용 결과. [bonusExp] 는 이벤트 덕에 더 받은 양이라 앱에서 "+N 보너스" 연출에 쓴다.
     */
    data class ExpBoost(
        val baseExp: Int,
        val grantedExp: Int,
        val multiplier: Double
    ) {
        val bonusExp: Int get() = grantedExp - baseExp

        companion object {
            /** 이벤트 없음 — 원본 그대로. */
            fun none(baseExp: Int): ExpBoost = ExpBoost(baseExp, baseExp, 1.0)
        }
    }
}
