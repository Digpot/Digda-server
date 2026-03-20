package digdaserver.domain.user.domain.repository

import digdaserver.domain.user.domain.entity.UserTerms
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserTermsRepository : JpaRepository<UserTerms, Long>
