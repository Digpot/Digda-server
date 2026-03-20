package digdaserver.domain.oauth2.application.service

import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.oauth2.presentation.dto.res.LoginToken

interface CreateAccessTokenAndRefreshTokenService {
    fun createAccessTokenAndRefreshToken(userId: String, role: Role, email: String): LoginToken
}
