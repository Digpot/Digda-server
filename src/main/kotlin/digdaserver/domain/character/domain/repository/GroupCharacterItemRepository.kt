package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacterItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterItemRepository : JpaRepository<GroupCharacterItem, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long): List<GroupCharacterItem>

    fun existsByGroupRoomIdAndShopItemId(groupRoomId: Long, shopItemId: Long): Boolean
}
