package digdaserver.admin.pii.application.service.impl

import digdaserver.admin.auth.domain.repository.AdminCredentialRepository
import digdaserver.admin.db.application.service.AdminDbService
import digdaserver.admin.pii.application.service.AdminPiiService
import digdaserver.admin.pii.presentation.dto.req.PiiTargetType
import digdaserver.admin.pii.presentation.dto.req.RevealPiiRequest
import digdaserver.admin.pii.presentation.dto.res.RevealPiiResponse
import digdaserver.domain.deletionrequest.domain.repository.DeletionRequestRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.security.AttemptLimiter
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminPiiServiceImpl(
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val attemptLimiter: AttemptLimiter,
    private val userRepository: UserRepository,
    private val deletionRequestRepository: DeletionRequestRepository,
    private val adminDbService: AdminDbService,
    private val userActionLogService: UserActionLogService
) : AdminPiiService {

    companion object {
        private const val MAX_FAILURES = 5
        private val LOCK_WINDOW: Duration = Duration.ofMinutes(10)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override fun reveal(adminUserId: UUID, request: RevealPiiRequest): RevealPiiResponse {
        verifyPassword(adminUserId, request.password)

        val fields: Map<String, Any?> = when (request.targetType) {
            PiiTargetType.USER -> {
                val userId = runCatching { UUID.fromString(request.targetId) }
                    .getOrElse { throw DigdaException(ErrorCode.INVALID_PARAMETER) }
                val user = userRepository.findById(userId)
                    .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
                linkedMapOf(
                    "name" to user.name,
                    "displayName" to user.displayName,
                    "email" to user.email
                )
            }
            PiiTargetType.DELETION_REQUEST -> {
                val id = request.targetId?.toLongOrNull() ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)
                val entity = deletionRequestRepository.findById(id)
                    .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
                linkedMapOf("email" to entity.email)
            }
            PiiTargetType.DB_ROW -> {
                val table = request.table ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)
                val pk = request.pk ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)
                adminDbService.revealRow(table, pk)
            }
        }

        // 누가 언제 누구의 원문을 봤는지는 반드시 남긴다 — 사후 추적의 유일한 근거.
        val target = request.targetId ?: "${request.table}:${request.pk}"
        log.warn("[PII-REVEAL] admin={} type={} target={}", adminUserId, request.targetType, target)
        userActionLogService.record(
            actorId = adminUserId,
            action = UserAction.OTHER,
            targetType = "PII_REVEAL",
            targetId = target.take(255),
            detail = "개인정보 원문 열람 (${request.targetType})"
        )
        return RevealPiiResponse(fields)
    }

    private fun verifyPassword(adminUserId: UUID, password: String) {
        val key = "pii-reveal:$adminUserId"
        if (attemptLimiter.isLocked(key, MAX_FAILURES)) {
            throw DigdaException(ErrorCode.TOO_MANY_ATTEMPTS)
        }
        val credential = adminCredentialRepository.findByUser_Id(adminUserId)
            .orElseThrow { DigdaException(ErrorCode.NOT_ADMIN_USER) }
        if (!passwordEncoder.matches(password, credential.password)) {
            attemptLimiter.recordFailure(key, LOCK_WINDOW)
            log.warn("[PII-REVEAL] 비밀번호 불일치 admin={}", adminUserId)
            // 401 을 쓰면 어드민 클라이언트가 토큰 만료로 보고 로그아웃시킨다.
            throw DigdaException(ErrorCode.PII_REVEAL_PASSWORD_MISMATCH)
        }
        attemptLimiter.reset(key)
    }
}
