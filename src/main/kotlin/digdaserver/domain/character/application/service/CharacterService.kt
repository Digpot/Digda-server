package digdaserver.domain.character.application.service

import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import java.util.UUID

/**
 * 캐릭터(모찌) 도메인. 그룹 1개당 1마리이며, 모든 메서드는 (요청자 userId, 대상 groupRoomId)
 * 를 받아 그룹 멤버십을 검증한 뒤 그룹 캐릭터를 조작한다.
 *
 * 외형(스킨/액세서리) 관련 작업은 [CharacterShopService] 로 분리되어 있다.
 */
interface CharacterService {

    /** 그룹의 캐릭터 상태. 첫 진입이면 자동 생성 후 반환. */
    fun getGroupCharacter(userId: UUID, groupRoomId: Long): CharacterStateResponse

    /**
     * 경험치 가산. amount 음수는 400 (가산 전용 — 차감은 별도 메서드를 둬야 함).
     * coinDelta 는 함께 지급되는 코인량. 0 가능.
     */
    fun gainExp(
        userId: UUID,
        groupRoomId: Long,
        amount: Int,
        coinDelta: Int,
        source: String?
    ): AddExpResponse

    /** 진화 트리 + 그룹 캐릭터 도달 현황. */
    fun getStageTree(userId: UUID, groupRoomId: Long): CharacterStageTreeResponse
}
