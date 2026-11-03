package digdaserver.admin.auth.application.service.impl

import digdaserver.admin.auth.application.service.AdminAuthService
import digdaserver.admin.auth.domain.repository.AdminCredentialRepository
import digdaserver.admin.auth.presentation.dto.req.AdminLoginRequest
import digdaserver.admin.auth.presentation.dto.res.AdminLoginResponse
import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
import digdaserver.domain.user.domain.entity.Role
import digdaserver.global.common.masking.PiiMasker
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.security.AttemptLimiter
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class AdminAuthServiceImpl(
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val createAccessTokenAndRefreshTokenService: CreateAccessTokenAndRefreshTokenService,
    private val attemptLimiter: AttemptLimiter
) : AdminAuthService {

    companion object {
        private const val MAX_FAILURES = 5
        private val LOCK_WINDOW: Duration = Duration.ofMinutes(10)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    /** 없는 계정이어도 bcrypt 비교 한 번만큼 시간을 쓰게 하는 더미 해시 — 응답 시간으로 계정 존재를 알 수 없게. */
    private val dummyHash: String by lazy { passwordEncoder.encode(java.util.UUID.randomUUID().toString()) }

    @Transactional
    override fun login(request: AdminLoginRequest): AdminLoginResponse {
        val email = request.email.trim().lowercase()
        val key = "admin-login:$email"
        if (attemptLimiter.isLocked(key, MAX_FAILURES)) {
            throw DigdaException(ErrorCode.TOO_MANY_ATTEMPTS)
        }

        // 계정 없음 / 비밀번호 틀림 / 관리자 아님을 같은 응답으로 돌려준다.
        // 구분해서 알려주면 관리자 이메일을 하나씩 대입해 찾아낼 수 있다.
        val credential = adminCredentialRepository.findByEmail(request.email).orElse(null)
        val matched = passwordEncoder.matches(request.password, credential?.password ?: dummyHash)
        val user = credential?.user
        if (credential == null || !matched || user?.role != Role.ADMIN) {
            attemptLimiter.recordFailure(key, LOCK_WINDOW)
            log.warn("[ADMIN-LOGIN] 실패 email={}", PiiMasker.email(email))
            throw DigdaException(ErrorCode.ADMIN_LOGIN_FAILED)
        }
        attemptLimiter.reset(key)

        val token = createAccessTokenAndRefreshTokenService
            .createAccessTokenAndRefreshToken(user.id.toString(), Role.ADMIN, user.email)

        return AdminLoginResponse.of(
            adminId = user.id.toString(),
            email = credential.email,
            name = user.name,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken
        )
    }
}
