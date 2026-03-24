package digdaserver.domain.membership.domain.repository

import digdaserver.domain.membership.domain.entity.Membership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface MembershipRepository : JpaRepository<Membership, Long> {

    fun findByGroupIdAndUserId(groupId: Long, userId: UUID): Optional<Membership>

    fun findAllByGroupId(groupId: Long): List<Membership>

    @Query("SELECT m FROM Membership m JOIN FETCH m.group g WHERE m.user.id = :userId AND g.deletedAt IS NULL ORDER BY g.lastActivityAt DESC")
    fun findAllByUserIdWithGroup(userId: UUID): List<Membership>

    fun existsByGroupIdAndUserId(groupId: Long, userId: UUID): Boolean

    fun countByGroupId(groupId: Long): Int

    fun deleteByGroupIdAndUserId(groupId: Long, userId: UUID)
}
