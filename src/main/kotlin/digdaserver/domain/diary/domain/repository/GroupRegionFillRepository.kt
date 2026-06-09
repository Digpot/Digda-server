package digdaserver.domain.diary.domain.repository

import digdaserver.domain.diary.domain.entity.GroupRegionFill
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRegionFillRepository : JpaRepository<GroupRegionFill, Long> {

    fun findAllByGroupRoomId(groupRoomId: Long): List<GroupRegionFill>

    fun existsByGroupRoomIdAndRegionKey(groupRoomId: Long, regionKey: String): Boolean

    fun deleteByGroupRoomIdAndRegionKey(groupRoomId: Long, regionKey: String)

    fun deleteByGroupRoomId(groupRoomId: Long)
}
