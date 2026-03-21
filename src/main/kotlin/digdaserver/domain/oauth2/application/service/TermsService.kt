package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.presentation.dto.req.TermsAgreeRequest
import digdaserver.domain.oauth2.presentation.dto.res.TermsAgreeResponse

interface TermsService {

    fun agreeToTerms(userId: Long, request: TermsAgreeRequest): TermsAgreeResponse
}
