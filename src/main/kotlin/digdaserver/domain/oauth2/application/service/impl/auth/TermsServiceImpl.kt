package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.oauth2.application.service.TermsService
import digdaserver.domain.oauth2.presentation.dto.req.TermsAgreeRequest
import digdaserver.domain.user.domain.entity.UserTerms
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Transactional
class TermsServiceImpl(
    private val userRepository: UserRepository
) : TermsService {

    override fun agreeToTerms(userId: UUID, request: TermsAgreeRequest) {
        if (!request.termsOfService || !request.privacyPolicy || !request.ageConfirmation) {
            throw DigdaException(ErrorCode.REQUIRED_TERMS_NOT_AGREED)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val terms = UserTerms(
            user = user,
            termsOfService = request.termsOfService,
            privacyPolicy = request.privacyPolicy,
            ageConfirmation = request.ageConfirmation,
            marketingConsent = request.marketingConsent,
            pushConsent = request.pushConsent
        )

        user.agreeToTerms(terms)

        // 푸시 동의 여부를 알림 설정에도 반영
        user.notificationSetting?.let {
            it.pushEnabled = request.pushConsent
            it.marketingConsent = request.marketingConsent
        }
    }
}
