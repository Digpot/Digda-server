package digdaserver.global.jwt.domain.repository

import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.global.jwt.domain.entity.SocialToken
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SocialTokenRepository : CrudRepository<SocialToken, String> {

    fun findByUserIdAndProvider(
        userId: String,
        provider: SocialProvider
    ): Optional<SocialToken>

    fun deleteByUserIdAndProvider(
        userId: String,
        provider: SocialProvider
    )

    fun deleteByUserId(userId: String)
}
