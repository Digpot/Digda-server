package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.UserCharacter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserCharacterRepository : JpaRepository<UserCharacter, Long> {
    fun findByUserId(userId: UUID): UserCharacter?
}
