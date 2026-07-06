package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ShopItemRepository : JpaRepository<ShopItem, Long> {

    fun findByItemKey(itemKey: String): ShopItem?

    /**
     * 노출 가능한 모든 아이템을 카테고리 → 정렬값 → ID 순으로 정렬.
     *
     * 메서드 이름 파생 쿼리(findAllByEnabledTrue...) 는 nested property 매핑이
     * 모호해질 수 있어 명시적 JPQL 로 작성.
     */
    @Query(
        "SELECT s FROM ShopItem s WHERE s.enabled = true " +
            "ORDER BY s.itemType ASC, s.sortOrder ASC, s.id ASC"
    )
    fun findAllEnabled(): List<ShopItem>

    @Query("SELECT s FROM ShopItem s WHERE s.isDefault = true")
    fun findAllDefaults(): List<ShopItem>

    @Query(
        "SELECT s FROM ShopItem s WHERE s.itemType = :itemType AND s.isDefault = true"
    )
    fun findFirstDefaultByItemType(@Param("itemType") itemType: ShopItemType): ShopItem?
}
