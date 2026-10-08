package digdaserver.admin.auth.domain.repository

import digdaserver.admin.auth.domain.entity.AdminCredential
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AdminCredentialRepository : JpaRepository<AdminCredential, Long> {

    fun findByEmail(email: String): Optional<AdminCredential>

    fun findByUser_Id(userId: UUID): Optional<AdminCredential>
}
