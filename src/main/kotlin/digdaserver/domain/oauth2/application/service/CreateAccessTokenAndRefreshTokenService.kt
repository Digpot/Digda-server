package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.presentation.dto.res.LoginToken
import digdaserver.domain.user.domain.entity.Role

interface CreateAccessTokenAndRefreshTokenService {
    fun createAccessTokenAndRefreshToken(userId: String, role: Role, email: String): LoginToken
}
