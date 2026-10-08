package digdaserver.admin.event.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.event.presentation.dto.req.GrantCoinRequest
import digdaserver.admin.event.presentation.dto.req.UpdateExpEventRequest
import digdaserver.admin.event.presentation.dto.res.CoinGrantResponse
import digdaserver.domain.event.presentation.dto.res.ExpEventResponse

/**
 * 시즌 이벤트 운영 — 경험치 배수 설정과 코인 일괄 지급.
 *
 * 두 기능 모두 어드민이 직접 방아쇠를 당기며, 코인 지급은 되돌릴 수 없으므로 실행 이력을
 * 남긴다(중복 지급 사고를 사람이 알아챌 수 있게).
 */
interface AdminEventService {

    fun getExpEvent(): ExpEventResponse

    /** 경험치 배수 이벤트 저장. 종료가 시작보다 빠르면 400. */
    fun updateExpEvent(request: UpdateExpEventRequest): ExpEventResponse

    /**
     * 살아있는 모든 그룹의 모찌에 코인을 지급한다. 아직 모찌 행이 없는 그룹은 행을 만들어
     * 함께 지급하므로 "캐릭터 화면에 안 들어가 본 그룹" 도 빠지지 않는다.
     */
    fun grantCoinToAll(request: GrantCoinRequest): CoinGrantResponse

    /** 지급 이력 (최신순). */
    fun searchCoinGrants(page: Int, size: Int): AdminPageResponse<CoinGrantResponse>
}
