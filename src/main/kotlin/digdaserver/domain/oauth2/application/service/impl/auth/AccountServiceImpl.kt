package digdaserver.domain.oauth2.application.service.impl.auth

import digdaserver.domain.group.domain.entity.GroupRole
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.oauth2.application.service.AccountService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.jwt.domain.repository.JsonWebTokenRepository
import digdaserver.global.jwt.domain.repository.SocialTokenRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Transactional
class AccountServiceImpl(
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipRepository,
    private val jsonWebTokenRepository: JsonWebTokenRepository,
    private val socialTokenRepository: SocialTokenRepository
) : AccountService {

    private val log = LoggerFactory.getLogger(AccountServiceImpl::class.java)

    override fun deleteAccount(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val memberships = membershipRepository.findAllByUserIdWithGroup(userId)
        val ownsActiveGroup = memberships.any { it.role == GroupRole.OWNER }

        if (ownsActiveGroup) {
            throw DigdaException(ErrorCode.OWNS_ACTIVE_GROUP)
        }

        // 토큰 무효화
        jsonWebTokenRepository.deleteByProviderId(userId.toString())
        socialTokenRepository.deleteByUserId(userId.toString())

        // 멤버십 삭제
        memberships.forEach { membership ->
            membershipRepository.delete(membership)
        }

        // 계정 삭제
        userRepository.delete(user)

        log.info("회원 탈퇴 완료: userId={}", userId)
    }
}
