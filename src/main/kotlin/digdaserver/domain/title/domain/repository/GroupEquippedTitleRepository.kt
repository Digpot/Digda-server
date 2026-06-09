package digdaserver.domain.title.domain.repository

import digdaserver.domain.title.domain.entity.GroupEquippedTitle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface GroupEquippedTitleRepository : JpaRepository<GroupEquippedTitle, Long> {

    fun findByGroupRoomId(groupRoomId: Long): Optional<GroupEquippedTitle>

    fun deleteByGroupRoomId(groupRoomId: Long)
}
