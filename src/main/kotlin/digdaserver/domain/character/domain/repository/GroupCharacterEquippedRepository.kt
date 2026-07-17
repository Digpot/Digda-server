package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.ShopItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterEquippedRepository : JpaRepository<GroupCharacterEquipped, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long): List<GroupCharacterEquipped>

    /** 특정 아이템을 장착 중인 모든 그룹 조회 — 아이템 retire 시 기본템 복귀용. */
    fun findAllByShopItemId(shopItemId: Long): List<GroupCharacterEquipped>

    fun findByGroupRoomIdAndItemType(
        groupRoomId: Long,
        itemType: ShopItemType
    ): GroupCharacterEquipped?

    /**
     * 카테고리 슬롯 해제.
     *
     * [Modifying.clearAutomatically] = true 로 1차 캐시(persistence context) 를 비워,
     * 같은 트랜잭션의 다음 [findAllByGroupRoomId] 가 stale 엔티티를 반환하지 않게 한다.
     * (unequip → 즉시 응답 빌드 패턴에서 필수)
     */
    @Modifying(clearAutomatically = true)
    @Query(
        "DELETE FROM GroupCharacterEquipped e " +
            "WHERE e.groupRoom.id = :groupRoomId AND e.itemType = :itemType"
    )
    fun deleteByGroupRoomIdAndItemType(
        @Param("groupRoomId") groupRoomId: Long,
        @Param("itemType") itemType: ShopItemType
    ): Int
}
