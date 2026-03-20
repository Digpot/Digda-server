package digdaserver.domain.invite.domain.repository

import digdaserver.domain.invite.domain.entity.InviteCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InviteCodeRepository : JpaRepository<InviteCode, Long> {

    fun findByCode(code: String): Optional<InviteCode>

    fun findFirstByGroupIdOrderByCreatedAtDesc(groupId: Long): Optional<InviteCode>

    fun deleteAllByGroupId(groupId: Long)
}
