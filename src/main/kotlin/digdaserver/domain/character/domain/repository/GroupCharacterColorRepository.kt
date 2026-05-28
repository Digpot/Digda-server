package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.domain.entity.GroupCharacterColor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterColorRepository : JpaRepository<GroupCharacterColor, Long> {
    fun findAllByGroupRoomId(groupRoomId: Long): List<GroupCharacterColor>
    fun existsByGroupRoomIdAndColor(groupRoomId: Long, color: CharacterColor): Boolean
}
