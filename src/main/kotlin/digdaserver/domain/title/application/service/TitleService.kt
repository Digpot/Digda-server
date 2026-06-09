package digdaserver.domain.title.application.service

import digdaserver.domain.title.presentation.dto.req.ClaimTitleItem
import digdaserver.domain.title.presentation.dto.res.EquippedTitleResponse
import digdaserver.domain.title.presentation.dto.res.TitleCatalogResponse
import digdaserver.domain.title.presentation.dto.res.TitleResponse
import java.util.UUID

interface TitleService {

    /** 칭호 카탈로그 전체(앱 렌더·획득 판정 메타). */
    fun catalog(): List<TitleCatalogResponse>

    /**
     * 사용자가 획득한 칭호 전체를 반환한다.
     * 조회 시점에 서버가 직접 셀 수 있는 칭호(작성 일기 수 누적)는 멱등하게 자동 적재한 뒤 함께 반환한다.
     */
    fun list(userId: UUID): List<TitleResponse>

    /**
     * 앱이 판정한 획득 칭호들을 멱등 적재한다(지역 정복·캐릭터 등 앱이 소유한 조건).
     * 이미 보유했거나, 멤버가 아닌 그룹·형식이 잘못된 코드는 조용히 건너뛴다.
     * 적재 후 사용자의 전체 칭호 목록을 반환한다.
     */
    fun claim(userId: UUID, items: List<ClaimTitleItem>): List<TitleResponse>

    /** 그룹 모찌에 장착된 칭호(없으면 code=null). */
    fun equippedTitle(groupRoomId: Long): EquippedTitleResponse

    /**
     * 그룹 모찌에 칭호를 장착/해제([code]=null 이면 해제).
     * 그룹 구성원만 가능하고, 본인이 획득한 칭호만 장착할 수 있다.
     */
    fun equipTitle(userId: UUID, groupRoomId: Long, code: String?): EquippedTitleResponse
}
