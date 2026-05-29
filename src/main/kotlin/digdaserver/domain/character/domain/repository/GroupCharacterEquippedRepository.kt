package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacterEquipped
import digdaserver.domain.character.domain.entity.ShopItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterEquippedRepository : JpaRepository<GroupCharacterEquipped, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long): List<GroupCharacterEquipped>

    fun findByGroupRoomIdAndItemType(
        groupRoomId: Long,
        itemType: ShopItemType
    ): GroupCharacterEquipped?

    fun deleteByGroupRoomIdAndItemType(groupRoomId: Long, itemType: ShopItemType): Long
}
