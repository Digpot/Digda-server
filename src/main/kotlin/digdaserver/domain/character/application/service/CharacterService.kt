package digdaserver.domain.character.application.service

import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterColorShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import java.util.UUID

interface CharacterService {

    /** 내 캐릭터 상태. 첫 진입이면 자동 생성 후 반환. */
    fun getMyCharacter(userId: UUID): CharacterStateResponse

    /**
     * 경험치 가산. amount 음수는 400 (가산 전용 — 차감은 별도 메서드를 둬야 함).
     * coinDelta 는 함께 지급되는 코인량. 0 가능.
     */
    fun gainExp(userId: UUID, amount: Int, coinDelta: Int, source: String?): AddExpResponse

    /** 진화 트리 + 내 도달 현황. */
    fun getStageTree(userId: UUID): CharacterStageTreeResponse

    /** 색상 상점 (전체 색상 + 내 보유/현재 여부). */
    fun getColorShop(userId: UUID): CharacterColorShopResponse

    /** 색상 구매. 잔액 부족·이미 보유 시 4xx. */
    fun buyColor(userId: UUID, color: CharacterColor): CharacterColorShopResponse

    /** 보유 중인 색상으로 변경. 미보유 시 4xx. */
    fun applyColor(userId: UUID, color: CharacterColor): CharacterStateResponse
}
