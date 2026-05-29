package digdaserver.domain.character.application.service

import digdaserver.domain.character.domain.entity.ShopItemType
import digdaserver.domain.character.presentation.dto.res.CharacterShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import java.util.UUID

/**
 * 캐릭터 상점 도메인 — 카테고리화된 아이템 구매 / 장착 / 해제.
 *
 * 모든 작업은 그룹 스코프이며, 호출자는 그룹 멤버여야 한다. 비용/잔액은 그룹 공용
 * 코인을 차감/검증한다.
 */
interface CharacterShopService {

    /** 전체 카테고리 + 그룹 보유/장착 여부 + 잔액. */
    fun getShop(userId: UUID, groupRoomId: Long): CharacterShopResponse

    /** 아이템 구매. 이미 보유 시 409, 코인 부족 시 400, 미존재 시 404. */
    fun buyItem(userId: UUID, groupRoomId: Long, itemKey: String): CharacterShopResponse

    /** 보유 아이템 장착. 같은 카테고리의 이전 장착은 덮어쓴다. */
    fun equipItem(userId: UUID, groupRoomId: Long, itemKey: String): CharacterStateResponse

    /**
     * 카테고리 슬롯 해제. SKIN 은 시스템상 반드시 1개 장착이 필요하므로 default 스킨으로
     * 강제 복귀. 그 외 카테고리는 row 가 제거된다.
     */
    fun unequipSlot(userId: UUID, groupRoomId: Long, itemType: ShopItemType): CharacterStateResponse
}
