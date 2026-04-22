package digdaserver.admin.auth.application.service.impl

import digdaserver.admin.auth.application.service.AdminAuthService
import digdaserver.admin.auth.domain.repository.AdminCredentialRepository
import digdaserver.admin.auth.presentation.dto.req.AdminLoginRequest
import digdaserver.admin.auth.presentation.dto.res.AdminLoginResponse
import digdaserver.admin.log.application.service.AdminActionLogService
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.domain.oauth2.application.service.CreateAccessTokenAndRefreshTokenService
import digdaserver.domain.user.domain.entity.Role
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAuthServiceImpl(
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val createAccessTokenAndRefreshTokenService: CreateAccessTokenAndRefreshTokenService,
    private val adminActionLogService: AdminActionLogService
) : AdminAuthService {

    @Transactional
    override fun login(request: AdminLoginRequest): AdminLoginResponse {
        val credential = adminCredentialRepository.findByEmail(request.email)
            .orElseThrow { DigdaException(ErrorCode.ADMIN_NOT_FOUND) }

        if (!passwordEncoder.matches(request.password, credential.password)) {
            throw DigdaException(ErrorCode.ADMIN_PASSWORD_MISMATCH)
        }

        val user = credential.user
        if (user.role != Role.ADMIN) {
            throw DigdaException(ErrorCode.NOT_ADMIN_USER)
        }

        val token = createAccessTokenAndRefreshTokenService
            .createAccessTokenAndRefreshToken(user.id.toString(), Role.ADMIN, user.email)

        adminActionLogService.record(
            actorId = user.id,
            action = AdminAction.LOGIN,
            targetType = "ADMIN",
            targetId = user.id.toString(),
            detail = "관리자 로그인: ${credential.email}"
        )

        return AdminLoginResponse.of(
            adminId = user.id.toString(),
            email = credential.email,
            name = user.name,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken
        )
    }
}
