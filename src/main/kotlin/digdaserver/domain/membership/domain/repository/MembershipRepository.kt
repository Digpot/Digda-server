package digdaserver.domain.membership.domain.repository

import digdaserver.domain.membership.domain.entity.Membership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface MembershipRepository : JpaRepository<Membership, Long> {

    fun findByGroupRoomIdAndUserId(groupRoomId: Long, userId: UUID): Optional<Membership>

    fun findAllByGroupRoomId(groupRoomId: Long): List<Membership>

    @Query("SELECT m FROM Membership m JOIN FETCH m.groupRoom g WHERE m.user.id = :userId AND g.deletedAt IS NULL ORDER BY g.lastActivityAt DESC")
    fun findAllByUserIdWithGroupRoom(userId: UUID): List<Membership>

    fun existsByGroupRoomIdAndUserId(groupRoomId: Long, userId: UUID): Boolean

    fun countByGroupRoomId(groupRoomId: Long): Int

    fun deleteByGroupRoomIdAndUserId(groupRoomId: Long, userId: UUID)
}
