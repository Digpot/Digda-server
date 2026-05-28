package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.GroupCharacter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupCharacterRepository : JpaRepository<GroupCharacter, Long> {
    fun findByGroupRoomId(groupRoomId: Long): GroupCharacter?
}
