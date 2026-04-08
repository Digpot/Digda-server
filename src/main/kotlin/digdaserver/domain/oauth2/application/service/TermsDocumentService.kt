package digdaserver.domain.oauth2.application.service

import digdaserver.domain.oauth2.presentation.dto.res.TermsDocumentResponse

interface TermsDocumentService {

    fun getTermsDocument(type: String): TermsDocumentResponse
}
