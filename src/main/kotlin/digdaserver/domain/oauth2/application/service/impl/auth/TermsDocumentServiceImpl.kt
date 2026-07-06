package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.oauth2.application.service.TermsDocumentService
import digdaserver.domain.oauth2.presentation.dto.res.TermsDocumentResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service

@Service
class TermsDocumentServiceImpl : TermsDocumentService {

    private val termsDocuments = mapOf(
        "terms-of-service" to TermsDocumentResponse(
            title = "이용약관",
            content = "<h1>DigDa 이용약관</h1><p>본 약관은 DigDa 서비스 이용에 관한 사항을 규정합니다.</p>",
            version = "v1.0",
            updatedAt = "2026-03-01"
        ),
        "privacy-policy" to TermsDocumentResponse(
            title = "개인정보처리방침",
            content = "<h1>개인정보처리방침</h1><p>DigDa는 사용자의 개인정보를 소중히 보호합니다.</p>",
            version = "v1.0",
            updatedAt = "2026-03-01"
        ),
        "marketing" to TermsDocumentResponse(
            title = "마케팅 정보 수신 동의",
            content = "<h1>마케팅 정보 수신 동의</h1><p>DigDa의 이벤트, 혜택 등 마케팅 정보를 수신합니다.</p>",
            version = "v1.0",
            updatedAt = "2026-03-01"
        )
    )

    override fun getTermsDocument(type: String): TermsDocumentResponse {
        return termsDocuments[type]
            ?: throw DigdaException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 약관 유형입니다: $type")
    }
}
