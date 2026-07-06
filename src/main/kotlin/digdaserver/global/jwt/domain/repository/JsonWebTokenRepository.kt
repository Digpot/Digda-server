package digdaserver.global.jwt.domain.repository

import digdaserver.global.jwt.domain.entity.JsonWebToken
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JsonWebTokenRepository : CrudRepository<JsonWebToken, String> {

    fun findByProviderId(providerId: String): List<JsonWebToken>

    fun deleteByProviderId(providerId: String)
}
