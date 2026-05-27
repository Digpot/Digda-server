package digdaserver.domain.character.domain.repository

import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.domain.entity.UserCharacterColor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserCharacterColorRepository : JpaRepository<UserCharacterColor, Long> {
    fun findAllByUserId(userId: UUID): List<UserCharacterColor>
    fun existsByUserIdAndColor(userId: UUID, color: CharacterColor): Boolean
}
