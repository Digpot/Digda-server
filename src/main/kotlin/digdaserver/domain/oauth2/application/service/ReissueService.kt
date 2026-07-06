package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.presentation.dto.res.LoginToken

interface ReissueService {
    fun reissue(refreshToken: String): LoginToken
}
