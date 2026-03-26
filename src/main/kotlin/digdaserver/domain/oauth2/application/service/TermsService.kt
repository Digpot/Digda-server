package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.presentation.dto.req.TermsAgreeRequest
import java.util.UUID

interface TermsService {

    fun agreeToTerms(userId: UUID, request: TermsAgreeRequest)
}
